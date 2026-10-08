package com.altafjava.school.application.saga;

import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.saga.SagaCoordinator;
import com.altafjava.platform.application.saga.SagaLifecycleService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.saga.repository.SagaLogRepository;
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the consequences of approving an {@link Admission} — creating the real Student
 * and Guardian records and notifying the tenant — as a saga so a partial failure (e.g. the
 * student's studentCode collides, or guardian linking fails) can be compensated cleanly instead
 * of leaving an admission in an inconsistent, half-enrolled state.
 *
 * <p>
 * This class is an orchestrator ({@code @Component}), not an application service — it legitimately
 * calls into {@link StudentService}/{@link GuardianService} (both are the tested, validated entry
 * points for creating those records) rather than duplicating their logic. CLAUDE.md's "application
 * services do not call other application services" rule governs services calling services; a
 * saga's entire purpose is cross-service orchestration, matching its own dedicated row in the
 * platform's Design Patterns table.
 *
 * Steps (in order):
 * 1. ENROLL_STUDENT — create the Student from the admission's applicant data
 * 2. LINK_GUARDIAN — create the Guardian from the admission's guardian contact info and link it
 * as the student's primary contact; this is the mechanism that establishes fee/billing
 * responsibility in this system's design (there is no separate BillingAccount entity — see
 * ROADMAP.md Phase 3)
 * 3. NOTIFY — tell tenant admins the enrollment completed
 * 4. MARK_ENROLLED — flip the admission's status to ENROLLED
 *
 * Compensation (reverse order): MARK_ENROLLED reverts the admission to APPROVED; NOTIFY is a
 * no-op (a sent notification can't be unsent); LINK_GUARDIAN soft-deletes the created guardian
 * link and guardian; ENROLL_STUDENT soft-deletes the created student.
 */
@Slf4j
@Component
public class AdmissionEnrollmentSaga extends SagaCoordinator {

	static final String SAGA_TYPE = "ADMISSION_ENROLLMENT";
	private static final int TOTAL_STEPS = 4;

	static final String STEP_ENROLL_STUDENT = "ENROLL_STUDENT";
	static final String STEP_LINK_GUARDIAN = "LINK_GUARDIAN";
	static final String STEP_NOTIFY = "NOTIFY";
	static final String STEP_MARK_ENROLLED = "MARK_ENROLLED";

	private final AdmissionRepository admissionRepository;
	private final StudentService studentService;
	private final GuardianService guardianService;
	private final TenantAdminNotifier tenantAdminNotifier;
	private final StudentRepository studentRepository;
	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final LifecycleRecorder lifecycleRecorder;

	public AdmissionEnrollmentSaga(SagaLifecycleService sagaLifecycleService, SagaLogRepository sagaLogRepository,
			AdmissionRepository admissionRepository, StudentService studentService, GuardianService guardianService,
			TenantAdminNotifier tenantAdminNotifier, StudentRepository studentRepository,
			GuardianRepository guardianRepository, StudentGuardianLinkRepository studentGuardianLinkRepository,
			LifecycleRecorder lifecycleRecorder) {
		super(sagaLifecycleService, sagaLogRepository);
		this.admissionRepository = admissionRepository;
		this.studentService = studentService;
		this.guardianService = guardianService;
		this.tenantAdminNotifier = tenantAdminNotifier;
		this.studentRepository = studentRepository;
		this.guardianRepository = guardianRepository;
		this.studentGuardianLinkRepository = studentGuardianLinkRepository;
		this.lifecycleRecorder = lifecycleRecorder;
	}

	// Deliberately not @Transactional — see the Javadoc note above and
	// AdmissionService.finalizeApproval(): each step below must commit on its own so a later
	// step's failure lets compensation observe and undo what earlier steps already did.
	public void enroll(Long admissionId, String studentCode) {
		Long tenantId = TenantContext.getCurrentTenantId();
		UUID tenantPublicId = TenantContext.getCurrentTenantPublicId();
		UUID sagaId = startSaga(SAGA_TYPE, tenantPublicId, TOTAL_STEPS);
		log.info("action=admission-enrollment-start sagaId={} admissionId={} tenantId={}", sagaId, admissionId,
				tenantId);

		Admission admission = admissionRepository.findByIdAndTenantId(admissionId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Admission not found: " + admissionId));
		admission.beginEnrollmentSaga(sagaId);
		// Each save() below commits independently (see the note above) and returns the entity
		// with its post-commit @Version bumped — reassigning is required, not stylistic: reusing
		// the pre-save reference for the next save() trips Hibernate's optimistic lock check
		// (StaleObjectStateException), since the in-memory version would be behind the DB's.
		admission = admissionRepository.save(admission);

		String currentStep = STEP_ENROLL_STUDENT;
		try {
			startStep(sagaId, STEP_ENROLL_STUDENT);
			Student student = studentService.enroll(studentCode, admission.getApplicantFirstName(),
					admission.getApplicantLastName(), null, admission.getApplicantDateOfBirth(), admission.getId());
			admission.recordEnrolledStudent(student.getId());
			admission = admissionRepository.save(admission);
			completeStep(sagaId, STEP_ENROLL_STUDENT, "studentId=" + student.getId());

			currentStep = STEP_LINK_GUARDIAN;
			startStep(sagaId, STEP_LINK_GUARDIAN);
			Guardian guardian = guardianService.create(admission.getGuardianFirstName(),
					admission.getGuardianLastName(), admission.getGuardianEmail(), admission.getGuardianPhone(),
					null);
			guardianService.linkToStudent(guardian.getPublicId().toString(), student.getPublicId().toString(),
					RelationshipType.OTHER, true);
			admission.recordEnrolledGuardian(guardian.getId());
			admission = admissionRepository.save(admission);
			completeStep(sagaId, STEP_LINK_GUARDIAN, "guardianId=" + guardian.getId());

			currentStep = STEP_NOTIFY;
			startStep(sagaId, STEP_NOTIFY);
			tenantAdminNotifier.notifyAll(tenantId, "Student Enrolled via Admission",
					"Admission for " + admission.getApplicantFirstName() + " " + admission.getApplicantLastName()
							+ " was approved and the student has been enrolled (code " + studentCode + ").");
			completeStep(sagaId, STEP_NOTIFY, "ok");

			currentStep = STEP_MARK_ENROLLED;
			startStep(sagaId, STEP_MARK_ENROLLED);
			admission.markEnrolled();
			admissionRepository.save(admission);
			completeStep(sagaId, STEP_MARK_ENROLLED, "ok");

			log.info("action=admission-enrollment-complete sagaId={} admissionId={} studentId={}", sagaId,
					admissionId, student.getId());
		} catch (RuntimeException ex) {
			log.error("action=admission-enrollment-failed sagaId={} admissionId={} step={}", sagaId, admissionId,
					currentStep, ex);
			failSaga(sagaId, currentStep, ex.getMessage());
			// Unlike TenantProvisioningSaga (triggered asynchronously off an event), this saga
			// runs synchronously from an admin's decide-approve request — the caller needs to see
			// the failure, not get a false 200, so the original exception is rethrown after
			// compensation runs.
			throw ex;
		}
	}

	@Override
	protected void compensateStep(UUID tenantId, UUID sagaId, String stepName) {
		Long schoolTenantId = TenantContext.getCurrentTenantId();
		log.info("action=admission-enrollment-compensate step={} sagaId={} tenantId={}", stepName, sagaId,
				schoolTenantId);

		Admission admission = admissionRepository.findByEnrollmentSagaIdAndTenantId(sagaId, schoolTenantId)
				.orElse(null);
		if (admission == null) {
			log.warn("action=admission-enrollment-compensate-skip reason=admission-not-found sagaId={}", sagaId);
			return;
		}

		// Steps are compensated in reverse completion order, and ENROLL_STUDENT — always the
		// first step to complete, if any did — is therefore always the last one compensated.
		// admission.revertEnrollment() must only run once all per-step undo work below has read
		// the tracking fields it needs, so it happens here, and only here.
		switch (stepName) {
			case STEP_MARK_ENROLLED, STEP_NOTIFY -> {
				// MARK_ENROLLED persisted only a status flip, reverted below regardless of which
				// step triggered compensation. A sent notification cannot be unsent.
			}
			case STEP_LINK_GUARDIAN -> {
				if (admission.getEnrolledGuardianId() != null) {
					studentGuardianLinkRepository.findByStudentId(schoolTenantId, admission.getEnrolledStudentId())
							.forEach(link -> {
								link.softDelete("saga-compensation");
								studentGuardianLinkRepository.save(link);
							});
					guardianRepository.findByIdAndTenantId(admission.getEnrolledGuardianId(), schoolTenantId)
							.ifPresent(guardian -> {
								guardian.softDelete("saga-compensation");
								guardianRepository.save(guardian);
							});
				}
			}
			case STEP_ENROLL_STUDENT -> {
				if (admission.getEnrolledStudentId() != null) {
					studentRepository.findByIdAndTenantId(admission.getEnrolledStudentId(), schoolTenantId)
							.ifPresent(student -> {
								student.softDelete("saga-compensation");
								studentRepository.save(student);
							});
				}
				lifecycleRecorder.enrollmentRolledBack(admission.getId(), admission.getEnrolledStudentId(),
						"Enrollment saga compensated");
				admission.revertEnrollment();
				admissionRepository.save(admission);
			}
			default -> log.warn("No compensation handler for step={}", stepName);
		}
	}
}

package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * Proves the admission-approval-to-enrollment saga is genuinely wired end to end against a real
 * Spring context and real DB: approving an admission creates a real Student and Guardian, links
 * them, and marks the admission ENROLLED; a failure during enrollment (a real, natural one — a
 * colliding student code, not a mock) leaves no orphaned Student and reverts the admission back
 * to APPROVED rather than a broken half-enrolled state — the literal "compensable steps" claim
 * from ROADMAP.md Phase 3, not a paraphrase of it.
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class, TestStorageConfig.class })
class AdmissionEnrollmentSagaIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private AdmissionService admissionService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	@Autowired
	private AdmissionRepository admissionRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private GuardianRepository guardianRepository;

	@Autowired
	private StudentGuardianLinkRepository studentGuardianLinkRepository;

	private Tenant tenant;

	@BeforeEach
	void createTenant() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenant = onboardingService.registerTenant(new RegisterTenantCommand(
				"Saga School", "saga-" + suffix, 1L, "admin@saga-" + suffix + ".test", "Password123!", "USD"));
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	// Uses finalizeApproval directly (not requestApproval, its @RequiresApproval-gated caller) —
	// this test is about the finalize-to-saga mechanics, not about whether a tenant happens to
	// have an ADMISSION_DECISION approval workflow seeded yet (SchoolTenantProvisioningListener
	// seeds one asynchronously, so racing that here would make requestApproval's outcome
	// nondeterministic — fine in production where either path converges on finalizeApproval, not
	// fine for a test asserting a specific return value).
	@Test
	void decide_approve_reallyCreatesStudentAndGuardianAndMarksAdmissionEnrolled() {
		String studentCode = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		Admission submitted = admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob-" + studentCode + "@family.test", "+14155552671", "Grade 3");

		Admission decided = admissionService.finalizeApproval(submitted.getPublicId().toString(), "admin", "approved",
				studentCode);

		assertEquals(AdmissionStatus.ENROLLED, decided.getStatus());
		var student = studentRepository.findByIdAndTenantId(decided.getEnrolledStudentId(), tenant.getId())
				.orElseThrow(() -> new AssertionError("Expected a real, persisted Student"));
		assertEquals(studentCode, student.getStudentCode());

		var guardian = guardianRepository.findByIdAndTenantId(decided.getEnrolledGuardianId(), tenant.getId())
				.orElseThrow(() -> new AssertionError("Expected a real, persisted Guardian"));
		assertTrue(studentGuardianLinkRepository
				.findByStudentId(tenant.getId(), student.getId()).stream()
				.anyMatch(link -> link.getGuardianId().equals(guardian.getId())),
				"Expected a real StudentGuardianLink between the new student and guardian");
	}

	@Test
	void decide_approveWithCollidingStudentCode_revertsAdmissionAndLeavesNoOrphanedStudent() {
		String collidingCode = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		studentService.enroll(collidingCode, "Existing", "Student", null, null);
		long studentCountBefore = studentRepository.findAllByTenantId(tenant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();

		Admission submitted = admissionService.submit("Carol", "White", LocalDate.of(2014, 6, 1), "Dave", "White",
				"dave-" + collidingCode + "@family.test", "+14155552672", "Grade 4");

		assertThrows(RuntimeException.class, () -> admissionService.finalizeApproval(
				submitted.getPublicId().toString(), "admin", "approved", collidingCode));

		Admission reloaded = admissionRepository.findByIdAndTenantId(submitted.getId(), tenant.getId()).orElseThrow();
		assertEquals(AdmissionStatus.APPROVED, reloaded.getStatus(),
				"A failed enrollment must revert the admission to APPROVED, not leave it stuck");
		long studentCountAfter = studentRepository.findAllByTenantId(tenant.getId(),
				org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();
		assertEquals(studentCountBefore, studentCountAfter,
				"A failed enrollment must not leave an orphaned Student behind");
	}
}

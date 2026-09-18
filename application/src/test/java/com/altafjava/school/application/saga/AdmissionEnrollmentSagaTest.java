package com.altafjava.school.application.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.saga.SagaLifecycleService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.saga.repository.SagaLogRepository;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionEnrollmentSagaTest {

	@Mock
	private SagaLifecycleService sagaLifecycleService;
	@Mock
	private SagaLogRepository sagaLogRepository;
	@Mock
	private AdmissionRepository admissionRepository;
	@Mock
	private StudentService studentService;
	@Mock
	private GuardianService guardianService;
	@Mock
	private TenantAdminNotifier tenantAdminNotifier;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private StudentGuardianLinkRepository studentGuardianLinkRepository;

	private AdmissionEnrollmentSaga saga;

	@BeforeEach
	void setUp() {
		saga = new AdmissionEnrollmentSaga(sagaLifecycleService, sagaLogRepository, admissionRepository,
				studentService, guardianService, tenantAdminNotifier, studentRepository, guardianRepository,
				studentGuardianLinkRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, UUID.randomUUID(), null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Admission admissionWithId(long id) {
		Admission admission = Admission.builder()
				.applicantFirstName("Alice")
				.applicantLastName("Smith")
				.applicantDateOfBirth(LocalDate.of(2015, 1, 1))
				.guardianFirstName("Bob")
				.guardianLastName("Smith")
				.guardianEmail("bob@family.test")
				.guardianPhone("555-1234")
				.appliedGrade("Grade 3")
				.status(AdmissionStatus.APPROVED)
				.submittedAt(Instant.now())
				.build();
		admission.setId(id);
		admission.setPublicId(UUID.randomUUID());
		return admission;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-100", "Alice", "Smith", null, LocalDate.of(2015, 1, 1));
		student.setId(id);
		student.setPublicId(UUID.randomUUID());
		return student;
	}

	private Guardian guardianWithId(long id) {
		Guardian guardian = Guardian.create("Bob", "Smith", "bob@family.test", "555-1234", null);
		guardian.setId(id);
		guardian.setPublicId(UUID.randomUUID());
		return guardian;
	}

	@Test
	void enroll_happyPath_completesAllStepsAndMarksAdmissionEnrolled() {
		Admission admission = admissionWithId(1L);
		Student student = studentWithId(10L);
		Guardian guardian = guardianWithId(20L);
		when(sagaLifecycleService.startSaga(any(), any(), eq(4))).thenReturn(UUID.randomUUID());
		when(admissionRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentService.enroll(eq("STU-100"), any(), any(), any(), any())).thenReturn(student);
		when(guardianService.create(any(), any(), any(), any(), any())).thenReturn(guardian);
		when(guardianService.linkToStudent(any(), any(), any(), eq(true)))
				.thenReturn(StudentGuardianLink.create(10L, 20L,
						com.altafjava.school.domain.guardian.model.RelationshipType.OTHER, true));

		saga.enroll(1L, "STU-100");

		assertEquals(AdmissionStatus.ENROLLED, admission.getStatus());
		assertEquals(10L, admission.getEnrolledStudentId());
		assertEquals(20L, admission.getEnrolledGuardianId());
		verify(tenantAdminNotifier).notifyAll(eq(1L), anyString(), anyString());
		verify(sagaLifecycleService, never()).markFailed(any(), any(), any());
	}

	@Test
	void enroll_whenStepFails_marksSagaFailedAndRethrowsOriginalException() {
		Admission admission = admissionWithId(1L);
		when(sagaLifecycleService.startSaga(any(), any(), eq(4))).thenReturn(UUID.randomUUID());
		when(admissionRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentService.enroll(eq("STU-100"), any(), any(), any(), any()))
				.thenThrow(new com.altafjava.platform.core.exception.BusinessException(
						"Student code already exists: STU-100"));
		when(sagaLogRepository.findById(any())).thenReturn(Optional.empty());

		RuntimeException ex = assertThrows(RuntimeException.class, () -> saga.enroll(1L, "STU-100"));

		assertTrue(ex.getMessage().contains("STU-100"));
		verify(sagaLifecycleService).markFailed(any(), eq(AdmissionEnrollmentSaga.STEP_ENROLL_STUDENT), anyString());
		verify(guardianService, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void compensateStep_enrollStudent_softDeletesStudentAndRevertsAdmission() {
		UUID sagaId = UUID.randomUUID();
		Admission admission = admissionWithId(1L);
		admission.recordEnrolledStudent(10L);
		Student student = studentWithId(10L);
		when(admissionRepository.findByEnrollmentSagaIdAndTenantId(sagaId, 1L)).thenReturn(Optional.of(admission));
		when(studentRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(student));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		saga.compensateStep(UUID.randomUUID(), sagaId, AdmissionEnrollmentSaga.STEP_ENROLL_STUDENT);

		assertTrue(student.isDeleted());
		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
		assertNull(admission.getEnrolledStudentId());
	}

	@Test
	void compensateStep_linkGuardian_softDeletesLinkAndGuardianButNotStudent() {
		UUID sagaId = UUID.randomUUID();
		Admission admission = admissionWithId(1L);
		admission.recordEnrolledStudent(10L);
		admission.recordEnrolledGuardian(20L);
		Guardian guardian = guardianWithId(20L);
		StudentGuardianLink link = StudentGuardianLink.create(10L, 20L,
				com.altafjava.school.domain.guardian.model.RelationshipType.OTHER, true);
		when(admissionRepository.findByEnrollmentSagaIdAndTenantId(sagaId, 1L)).thenReturn(Optional.of(admission));
		when(studentGuardianLinkRepository.findByStudentId(1L, 10L)).thenReturn(List.of(link));
		when(guardianRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(guardian));

		saga.compensateStep(UUID.randomUUID(), sagaId, AdmissionEnrollmentSaga.STEP_LINK_GUARDIAN);

		assertTrue(link.isDeleted());
		assertTrue(guardian.isDeleted());
		// LINK_GUARDIAN's own compensation must not prematurely clear tracking fields —
		// that happens only in ENROLL_STUDENT's compensation, which always runs last.
		verify(admissionRepository, never()).save(any());
	}

	@Test
	void compensateStep_notify_isNoOp() {
		UUID sagaId = UUID.randomUUID();
		Admission admission = admissionWithId(1L);
		when(admissionRepository.findByEnrollmentSagaIdAndTenantId(sagaId, 1L)).thenReturn(Optional.of(admission));

		saga.compensateStep(UUID.randomUUID(), sagaId, AdmissionEnrollmentSaga.STEP_NOTIFY);

		verify(admissionRepository, never()).save(any());
		verify(studentRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	void compensateStep_admissionNotFound_skipsWithoutError() {
		UUID sagaId = UUID.randomUUID();
		when(admissionRepository.findByEnrollmentSagaIdAndTenantId(sagaId, 1L)).thenReturn(Optional.empty());

		saga.compensateStep(UUID.randomUUID(), sagaId, AdmissionEnrollmentSaga.STEP_ENROLL_STUDENT);

		verify(studentRepository, never()).findByIdAndTenantId(any(), any());
	}
}

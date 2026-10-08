package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeDiscountRepository;
import com.altafjava.school.domain.fee.repository.FeeInstallmentRepository;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeRefundRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeePaymentServiceTest {

	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private FeeAssignmentRepository feeAssignmentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private NumberSequenceService numberSequenceService;
	@Mock
	private FeeDiscountRepository feeDiscountRepository;
	@Mock
	private FeeInstallmentRepository feeInstallmentRepository;
	@Mock
	private FeeRefundRepository feeRefundRepository;

	private FeePaymentService feePaymentService;

	@BeforeEach
	void setUp() {
		feePaymentService = new FeePaymentService(feePaymentRepository, studentRepository, feeStructureRepository,
				feeAssignmentRepository, studentClassroomLinkRepository, studentDataAccessGuard,
				numberSequenceService, feeDiscountRepository, feeInstallmentRepository, feeRefundRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void record_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> feePaymentService.record(99L, 1L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));

		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void record_withNonExistentFeeStructure_throwsResourceNotFound() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(99L, 1L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class,
				() -> feePaymentService.record(1L, 99L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));

		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void record_duplicateReceiptNumber_throwsIllegalArgument() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(feePaymentRepository.existsByReceiptNumberAndTenantId("RCPT-001", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> feePaymentService.record(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-001"));
	}

	@Test
	void record_withValidReferences_succeeds() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(feePaymentRepository.existsByReceiptNumberAndTenantId("RCPT-001", 1L)).thenReturn(false);
		when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> feePaymentService.record(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(),
				"RCPT-001"));
	}

	@Test
	void record_withoutReceiptNumber_generatesOneFromTenantSequence() {
		when(studentRepository.existsByIdAndTenantId(1L, 1L)).thenReturn(true);
		when(feeStructureRepository.existsByIdAndTenantId(2L, 1L)).thenReturn(true);
		when(numberSequenceService.generateNext(1L, "FEE_RECEIPT", "RCPT-", 6, ResetPeriod.YEARLY))
				.thenReturn("RCPT-2026-000001");
		when(feePaymentRepository.existsByReceiptNumberAndTenantId("RCPT-2026-000001", 1L)).thenReturn(false);
		when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(inv -> inv.getArgument(0));

		FeePayment payment = feePaymentService.record(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(), null);

		assertEquals("RCPT-2026-000001", payment.getReceiptNumber());
	}

	@Test
	void calculateBalance_withPartialPayment_returnsOutstandingBalance() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(1L);
		var feeStructure = com.altafjava.school.domain.fee.model.FeeStructure.create(
				"Tuition", BigDecimal.valueOf(1000), com.altafjava.school.domain.fee.model.FeeFrequency.MONTHLY,
				"Standard");
		feeStructure.setId(2L);
		var payment = FeePayment.create(1L, 2L, BigDecimal.valueOf(400), LocalDateTime.now(), "RCPT-100");
		var assignment = FeeAssignment.forStudent(2L, student.getId());

		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(java.util.Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(feeAssignmentRepository.findByTenantIdAndStudentId(1L, student.getId()))
				.thenReturn(java.util.List.of(assignment));
		when(studentClassroomLinkRepository.findByStudentId(1L, student.getId())).thenReturn(java.util.List.of());
		when(feeStructureRepository.findAllByIdInAndTenantId(java.util.List.of(2L), 1L))
				.thenReturn(java.util.List.of(feeStructure));
		when(feePaymentRepository.findByStudentId(1L, student.getId())).thenReturn(java.util.List.of(payment));

		var balances = feePaymentService.calculateBalance("11111111-1111-1111-1111-111111111111");

		assertEquals(1, balances.size());
		assertEquals(BigDecimal.valueOf(600), balances.get(0).outstandingBalance());
	}

	@Test
	void calculateBalance_pastDueDateAndGracePeriod_includesLateFeeInOutstanding() {
		var student = com.altafjava.school.domain.student.model.Student.create(
				"STU-2", "Carol", "White", "carol@school.test", null);
		student.setId(2L);
		var feeStructure = com.altafjava.school.domain.fee.model.FeeStructure.create(
				"Tuition", BigDecimal.valueOf(1000), com.altafjava.school.domain.fee.model.FeeFrequency.MONTHLY,
				"Standard");
		feeStructure.setId(3L);
		feeStructure.configureLateFeePolicy(5, BigDecimal.valueOf(10));
		var assignment = FeeAssignment.forStudent(3L, student.getId());
		assignment.configureDueDate(LocalDate.now().minusDays(30), null, null);

		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(java.util.Optional.of(student));
		org.mockito.Mockito.doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(feeAssignmentRepository.findByTenantIdAndStudentId(1L, student.getId()))
				.thenReturn(java.util.List.of(assignment));
		when(studentClassroomLinkRepository.findByStudentId(1L, student.getId())).thenReturn(java.util.List.of());
		when(feeStructureRepository.findAllByIdInAndTenantId(java.util.List.of(3L), 1L))
				.thenReturn(java.util.List.of(feeStructure));
		when(feePaymentRepository.findByStudentId(1L, student.getId())).thenReturn(java.util.List.of());

		var balances = feePaymentService.calculateBalance("11111111-1111-1111-1111-111111111111");

		assertEquals(1, balances.size());
		assertEquals(0, BigDecimal.valueOf(100).compareTo(balances.get(0).lateFeeAmount()));
		assertEquals(0, BigDecimal.valueOf(1100).compareTo(balances.get(0).outstandingBalance()));
	}

	@Test
	void calculateBalancesForStudents_multipleStudents_usesOneBatchedQueryPerRepository() {
		var studentA = com.altafjava.school.domain.student.model.Student.create(
				"STU-A", "Alice", "Smith", "alice@school.test", null);
		studentA.setId(1L);
		var studentB = com.altafjava.school.domain.student.model.Student.create(
				"STU-B", "Bob", "Jones", "bob@school.test", null);
		studentB.setId(2L);

		var tuition = com.altafjava.school.domain.fee.model.FeeStructure.create(
				"Tuition", BigDecimal.valueOf(1000), com.altafjava.school.domain.fee.model.FeeFrequency.MONTHLY,
				"Standard");
		tuition.setId(10L);
		var transport = com.altafjava.school.domain.fee.model.FeeStructure.create(
				"Transport", BigDecimal.valueOf(200), com.altafjava.school.domain.fee.model.FeeFrequency.MONTHLY,
				"Standard");
		transport.setId(11L);

		// A is directly assigned tuition; B gets transport via their classroom's assignment.
		var directAssignment = FeeAssignment.forStudent(10L, 1L);
		var classroomAssignment = FeeAssignment.forClassroom(11L, 5L);
		var link = com.altafjava.school.domain.classroom.model.StudentClassroomLink.create(2L, 5L, 1L,
				java.time.LocalDate.of(2026, 1, 1));
		var paymentForA = FeePayment.create(1L, 10L, BigDecimal.valueOf(300), LocalDateTime.now(), "RCPT-A");

		when(studentClassroomLinkRepository.findByStudentIdIn(1L, java.util.List.of(1L, 2L)))
				.thenReturn(java.util.List.of(link));
		when(feeAssignmentRepository.findByTenantIdAndStudentIdIn(1L, java.util.List.of(1L, 2L)))
				.thenReturn(java.util.List.of(directAssignment));
		when(feeAssignmentRepository.findByTenantIdAndClassroomIdIn(1L, java.util.List.of(5L)))
				.thenReturn(java.util.List.of(classroomAssignment));
		when(feeStructureRepository.findAllByIdInAndTenantId(
				org.mockito.ArgumentMatchers.argThat(ids -> ids.containsAll(java.util.List.of(10L, 11L))), any()))
				.thenReturn(java.util.List.of(tuition, transport));
		when(feePaymentRepository.findByStudentIdIn(1L, java.util.List.of(1L, 2L)))
				.thenReturn(java.util.List.of(paymentForA));

		var balancesByStudentId = feePaymentService.calculateBalancesForStudents(1L,
				java.util.List.of(studentA, studentB));

		assertEquals(1, balancesByStudentId.get(1L).size());
		assertEquals(BigDecimal.valueOf(700), balancesByStudentId.get(1L).get(0).outstandingBalance());
		assertEquals(1, balancesByStudentId.get(2L).size());
		assertEquals(BigDecimal.valueOf(200), balancesByStudentId.get(2L).get(0).outstandingBalance());

		verify(studentClassroomLinkRepository).findByStudentIdIn(any(), any());
		verify(feeAssignmentRepository).findByTenantIdAndStudentIdIn(any(), any());
		verify(feeAssignmentRepository).findByTenantIdAndClassroomIdIn(any(), any());
		verify(feePaymentRepository).findByStudentIdIn(any(), any());
	}
}

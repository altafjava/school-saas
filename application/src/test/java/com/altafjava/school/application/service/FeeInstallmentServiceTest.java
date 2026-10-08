package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
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
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeInstallmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeInstallmentServiceTest {

	private static final UUID STUDENT_ID = UUID.randomUUID();
	private static final UUID STRUCTURE_ID = UUID.randomUUID();

	@Mock
	private FeeInstallmentRepository feeInstallmentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;

	private FeeInstallmentService service;

	@BeforeEach
	void setUp() {
		service = new FeeInstallmentService(feeInstallmentRepository, studentRepository, feeStructureRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		Student student = Student.create("STU-1", "A", "B", null, LocalDate.of(2010, 1, 1));
		student.setId(5L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_ID, 1L)).thenReturn(Optional.of(student));
		FeeStructure structure = FeeStructure.create("Tuition", new BigDecimal("1000"), FeeFrequency.ANNUAL, null);
		structure.setId(6L);
		when(feeStructureRepository.findByPublicIdAndTenantId(STRUCTURE_ID, 1L)).thenReturn(Optional.of(structure));
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void setEvenPlan_softDeletesTheOldPlanAndSavesTheNewOne() {
		FeeInstallment old = FeeInstallment.of(5L, 6L, 1, LocalDate.of(2026, 1, 1), 10_000);
		when(feeInstallmentRepository.findByStudentAndStructure(1L, 5L, 6L)).thenReturn(List.of(old));
		when(feeInstallmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

		List<FeeInstallment> plan = service.setEvenPlan(STUDENT_ID.toString(), STRUCTURE_ID.toString(), 4,
				LocalDate.of(2026, 7, 1), 3);

		assertTrue(old.isDeleted());
		assertEquals(4, plan.size());
		assertEquals(10_000, plan.stream().mapToInt(FeeInstallment::getShareBasisPoints).sum());
	}

	@Test
	void removePlan_clearsTheExistingInstallmentsOnly() {
		FeeInstallment old = FeeInstallment.of(5L, 6L, 1, LocalDate.of(2026, 1, 1), 10_000);
		when(feeInstallmentRepository.findByStudentAndStructure(1L, 5L, 6L)).thenReturn(List.of(old));
		when(feeInstallmentRepository.saveAll(anyList())).thenReturn(List.of());

		service.removePlan(STUDENT_ID.toString(), STRUCTURE_ID.toString());

		assertTrue(old.isDeleted());
		verify(feeInstallmentRepository).saveAll(List.of());
	}
}

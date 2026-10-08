package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.fee.model.DiscountType;
import com.altafjava.school.domain.fee.model.FeeDiscount;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeDiscountRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeDiscountServiceTest {

	private static final UUID STUDENT_ID = UUID.randomUUID();
	private static final UUID STRUCTURE_ID = UUID.randomUUID();

	@Mock
	private FeeDiscountRepository feeDiscountRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;

	private FeeDiscountService service;

	@BeforeEach
	void setUp() {
		service = new FeeDiscountService(feeDiscountRepository, studentRepository, feeStructureRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		Student student = Student.create("STU-1", "A", "B", null, LocalDate.of(2010, 1, 1));
		student.setId(5L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_ID, 1L)).thenReturn(Optional.of(student));
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
	}

	private void structure() {
		FeeStructure structure = FeeStructure.create("Tuition", new BigDecimal("1000.00"), FeeFrequency.ANNUAL, null);
		structure.setId(6L);
		when(feeStructureRepository.findByPublicIdAndTenantId(STRUCTURE_ID, 1L)).thenReturn(Optional.of(structure));
	}

	@Test
	void grant_savesTheDiscountWithANormalisedCategory() {
		structure();
		when(feeDiscountRepository.findActiveByStudentAndStructure(1L, 5L, 6L)).thenReturn(List.of());
		when(feeDiscountRepository.save(any(FeeDiscount.class))).thenAnswer(inv -> inv.getArgument(0));

		FeeDiscount discount = service.grant(STUDENT_ID.toString(), STRUCTURE_ID.toString(), DiscountType.PERCENTAGE,
				new BigDecimal("20"), "sibling", "second child", 9L);

		assertEquals("SIBLING", discount.getCategory());
		assertEquals(9L, discount.getGrantedByUserId());
	}

	@Test
	void grant_refusesDiscountsThatTogetherExceedTheFee() {
		structure();
		FeeDiscount existing = FeeDiscount.grant(5L, 6L, DiscountType.FIXED, new BigDecimal("900"), "STAFF_WARD", "x",
				9L);
		when(feeDiscountRepository.findActiveByStudentAndStructure(1L, 5L, 6L)).thenReturn(List.of(existing));

		assertThrows(BusinessException.class, () -> service.grant(STUDENT_ID.toString(), STRUCTURE_ID.toString(),
				DiscountType.PERCENTAGE, new BigDecimal("20"), "SIBLING", "x", 9L));

		verify(feeDiscountRepository, never()).save(any());
	}

	@Test
	void revoke_ofAnotherStudentsDiscount_isNotFound() {
		UUID discountId = UUID.randomUUID();
		FeeDiscount others = FeeDiscount.grant(99L, 6L, DiscountType.FIXED, new BigDecimal("10"), "X1", "x", 9L);
		when(feeDiscountRepository.findByPublicIdAndTenantId(discountId, 1L)).thenReturn(Optional.of(others));

		assertThrows(ResourceNotFoundException.class,
				() -> service.revoke(STUDENT_ID.toString(), discountId.toString(), "mistake"));
	}

	@Test
	void revoke_endsTheStudentsOwnDiscount() {
		UUID discountId = UUID.randomUUID();
		FeeDiscount own = FeeDiscount.grant(5L, 6L, DiscountType.FIXED, new BigDecimal("10"), "X1", "x", 9L);
		when(feeDiscountRepository.findByPublicIdAndTenantId(discountId, 1L)).thenReturn(Optional.of(own));
		when(feeDiscountRepository.save(any(FeeDiscount.class))).thenAnswer(inv -> inv.getArgument(0));

		assertEquals(false,
				service.revoke(STUDENT_ID.toString(), discountId.toString(), "no longer eligible").isActive());
	}
}

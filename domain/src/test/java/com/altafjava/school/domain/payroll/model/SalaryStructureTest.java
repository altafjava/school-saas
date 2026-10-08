package com.altafjava.school.domain.payroll.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class SalaryStructureTest {

	private List<PayComponentAmount> components() {
		return List.of(
				new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, BigDecimal.valueOf(50000)),
				new PayComponentAmount("HRA", "House Rent Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(10000)),
				new PayComponentAmount("TRANSPORT", "Transport Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(2000)),
				new PayComponentAmount("OTHER_ALLOWANCE", "Other Allowances", PayComponentType.EARNING,
						BigDecimal.valueOf(500)),
				new PayComponentAmount("OTHER_DEDUCTION", "Other Deductions", PayComponentType.DEDUCTION,
						BigDecimal.valueOf(1000)));
	}

	private SalaryStructure structure() {
		return SalaryStructure.create(1L, components(), LocalDate.of(2026, 1, 1));
	}

	@Test
	void create_isActiveByDefault() {
		SalaryStructure structure = structure();

		assertTrue(structure.isActive());
		assertEquals(1L, structure.getEmployeeId());
	}

	@Test
	void create_withEmptyComponents_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> SalaryStructure.create(1L, List.of(), LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withZeroGrossPay_throwsBusinessException() {
		List<PayComponentAmount> zeroEarning = List.of(
				new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, BigDecimal.ZERO));

		assertThrows(BusinessException.class,
				() -> SalaryStructure.create(1L, zeroEarning, LocalDate.of(2026, 1, 1)));
	}

	@Test
	void create_withOnlyDeductionComponents_throwsBusinessException() {
		List<PayComponentAmount> onlyDeduction = List.of(
				new PayComponentAmount("OTHER_DEDUCTION", "Other Deductions", PayComponentType.DEDUCTION,
						BigDecimal.valueOf(1000)));

		assertThrows(BusinessException.class,
				() -> SalaryStructure.create(1L, onlyDeduction, LocalDate.of(2026, 1, 1)));
	}

	@Test
	void deactivate_flipsActiveFlag() {
		SalaryStructure structure = structure();

		structure.deactivate();

		assertFalse(structure.isActive());
	}

	@Test
	void grossPay_sumsEarningComponentsOnly() {
		SalaryStructure structure = structure();

		BigDecimal grossPay = structure.grossPay();

		assertEquals(0, BigDecimal.valueOf(62500).compareTo(grossPay));
	}

	@Test
	void toSnapshot_copiesAllLineItems() {
		SalaryStructure structure = structure();

		SalarySnapshot snapshot = structure.toSnapshot();

		assertEquals(components().size(), snapshot.components().size());
		assertEquals(0, BigDecimal.valueOf(62500).compareTo(snapshot.grossPay()));
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(snapshot.totalDeductions()));
	}
}

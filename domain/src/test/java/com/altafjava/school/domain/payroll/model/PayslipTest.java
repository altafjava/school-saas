package com.altafjava.school.domain.payroll.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class PayslipTest {

	private SalarySnapshot snapshot() {
		return new SalarySnapshot(List.of(
				new PayComponentAmount("BASIC", "Basic Pay", PayComponentType.EARNING, BigDecimal.valueOf(50000)),
				new PayComponentAmount("HRA", "House Rent Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(10000)),
				new PayComponentAmount("TRANSPORT", "Transport Allowance", PayComponentType.EARNING,
						BigDecimal.valueOf(2000)),
				new PayComponentAmount("OTHER_ALLOWANCE", "Other Allowances", PayComponentType.EARNING,
						BigDecimal.valueOf(500)),
				new PayComponentAmount("OTHER_DEDUCTION", "Other Deductions", PayComponentType.DEDUCTION,
						BigDecimal.valueOf(1000))));
	}

	private PayrollComputation computation() {
		return new PayrollComputation(BigDecimal.valueOf(62500), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.valueOf(61500));
	}

	private Payslip payslip() {
		return Payslip.generate(1L, 2026, 6, snapshot(), computation());
	}

	@Test
	void generate_startsInDraftStatus() {
		Payslip payslip = payslip();

		assertEquals(PayslipStatus.DRAFT, payslip.getStatus());
		assertEquals(2026, payslip.getPayYear());
		assertEquals(6, payslip.getPayMonth());
		assertEquals(0, BigDecimal.valueOf(61500).compareTo(payslip.getNetPay()));
		assertEquals(5, payslip.getComponents().size());
	}

	@Test
	void finalizePayslip_fromDraft_setsFinalizedStatus() {
		Payslip payslip = payslip();

		payslip.finalizePayslip(LocalDateTime.now());

		assertEquals(PayslipStatus.FINALIZED, payslip.getStatus());
		assertNotNull(payslip.getFinalizedAt());
	}

	@Test
	void finalizePayslip_whenNotDraft_throwsBusinessException() {
		Payslip payslip = payslip();
		payslip.finalizePayslip(LocalDateTime.now());

		assertThrows(BusinessException.class, () -> payslip.finalizePayslip(LocalDateTime.now()));
	}

	@Test
	void markDisbursed_fromFinalized_setsDisbursedStatus() {
		Payslip payslip = payslip();
		payslip.finalizePayslip(LocalDateTime.now());

		payslip.markDisbursed(LocalDateTime.now());

		assertEquals(PayslipStatus.DISBURSED, payslip.getStatus());
		assertNotNull(payslip.getDisbursedAt());
	}

	@Test
	void markDisbursed_whenStillDraft_throwsBusinessException() {
		Payslip payslip = payslip();

		assertThrows(BusinessException.class, () -> payslip.markDisbursed(LocalDateTime.now()));
	}

	@Test
	void markDisbursed_whenAlreadyDisbursed_throwsBusinessException() {
		Payslip payslip = payslip();
		payslip.finalizePayslip(LocalDateTime.now());
		payslip.markDisbursed(LocalDateTime.now());

		assertThrows(BusinessException.class, () -> payslip.markDisbursed(LocalDateTime.now()));
	}
}

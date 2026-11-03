package com.altafjava.school.domain.fee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeStructure;

class FeeBalanceCalculatorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

	private final FeeBalanceCalculator calculator = new FeeBalanceCalculator();

	private FeeStructure feeStructure(BigDecimal amount) {
		FeeStructure feeStructure = FeeStructure.create("Tuition", amount, FeeFrequency.MONTHLY, "Standard");
		feeStructure.setId(1L);
		return feeStructure;
	}

	@Test
	void calculate_noPayments_fullAmountOutstanding() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), null, null, TODAY);

		assertEquals(BigDecimal.valueOf(1000), balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
		assertEquals(BigDecimal.ZERO, balance.amountPaid());
		assertEquals(BigDecimal.ZERO, balance.lateFeeAmount());
	}

	@Test
	void calculate_partialPayment_reducesOutstandingBalance() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), null,
				BigDecimal.valueOf(400), TODAY);

		assertEquals(BigDecimal.valueOf(600), balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
	}

	@Test
	void calculate_exactPayment_zeroOutstandingAndZeroOverpaid() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), null,
				BigDecimal.valueOf(1000), TODAY);

		assertEquals(BigDecimal.ZERO, balance.outstandingBalance());
		assertEquals(BigDecimal.ZERO, balance.overpaidAmount());
	}

	@Test
	void calculate_overpayment_reportsOverpaidAmountExplicitlyNotAsNegativeBalance() {
		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.valueOf(1000)), null,
				BigDecimal.valueOf(1200), TODAY);

		assertEquals(BigDecimal.ZERO, balance.outstandingBalance());
		assertEquals(BigDecimal.valueOf(200), balance.overpaidAmount());
	}

	@Test
	void calculate_noDueDateOnAssignment_neverAppliesLateFeeEvenIfStructureHasPolicy() {
		FeeStructure feeStructure = feeStructure(BigDecimal.valueOf(1000));
		feeStructure.configureLateFeePolicy(0, BigDecimal.valueOf(10));
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);

		FeeBalance balance = calculator.calculate(feeStructure, assignment, null, TODAY);

		assertEquals(BigDecimal.ZERO, balance.lateFeeAmount());
		assertEquals(BigDecimal.valueOf(1000), balance.outstandingBalance());
	}

	@Test
	void calculate_pastDueDateAndGracePeriod_appliesLateFeeFromStructureDefault() {
		FeeStructure feeStructure = feeStructure(BigDecimal.valueOf(1000));
		feeStructure.configureLateFeePolicy(5, BigDecimal.valueOf(10));
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);
		assignment.configureDueDate(TODAY.minusDays(10), null, null);

		FeeBalance balance = calculator.calculate(feeStructure, assignment, null, TODAY);

		assertEquals(0, BigDecimal.valueOf(100).compareTo(balance.lateFeeAmount()));
		assertEquals(0, BigDecimal.valueOf(1100).compareTo(balance.outstandingBalance()));
	}

	@Test
	void calculate_withinGracePeriod_noLateFeeYet() {
		FeeStructure feeStructure = feeStructure(BigDecimal.valueOf(1000));
		feeStructure.configureLateFeePolicy(10, BigDecimal.valueOf(10));
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);
		assignment.configureDueDate(TODAY.minusDays(5), null, null);

		FeeBalance balance = calculator.calculate(feeStructure, assignment, null, TODAY);

		assertEquals(BigDecimal.ZERO, balance.lateFeeAmount());
		assertEquals(0, BigDecimal.valueOf(1000).compareTo(balance.outstandingBalance()));
	}

	@Test
	void calculate_assignmentOverrideBeatsStructureDefault() {
		FeeStructure feeStructure = feeStructure(BigDecimal.valueOf(1000));
		feeStructure.configureLateFeePolicy(0, BigDecimal.valueOf(10));
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);
		assignment.configureDueDate(TODAY.minusDays(10), 0, BigDecimal.valueOf(20));

		FeeBalance balance = calculator.calculate(feeStructure, assignment, null, TODAY);

		assertEquals(0, BigDecimal.valueOf(200).compareTo(balance.lateFeeAmount()));
	}

	@Test
	void calculate_noOutstandingBalance_neverAppliesLateFeeEvenIfOverdue() {
		FeeStructure feeStructure = feeStructure(BigDecimal.valueOf(1000));
		feeStructure.configureLateFeePolicy(0, BigDecimal.valueOf(10));
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 2L);
		assignment.configureDueDate(TODAY.minusDays(10), null, null);

		FeeBalance balance = calculator.calculate(feeStructure, assignment, BigDecimal.valueOf(1000), TODAY);

		assertEquals(BigDecimal.ZERO, balance.lateFeeAmount());
	}

	@Test
	void calculate_withAnIndividualAmount_billsThatInsteadOfTheStructure() {
		FeeAssignment openingBalance = FeeAssignment.forStudent(1L, 7L);
		openingBalance.chargeIndividually(new BigDecimal("2350.50"));

		FeeBalance balance = calculator.calculate(feeStructure(BigDecimal.ZERO), openingBalance,
				new BigDecimal("350.50"), TODAY);

		assertEquals(0, new BigDecimal("2350.50").compareTo(balance.grossAmount()));
		assertEquals(0, new BigDecimal("2000.00").compareTo(balance.outstandingBalance()));
	}

	@Test
	void chargeIndividually_onAClassroomFee_throwsBusinessException() {
		org.junit.jupiter.api.Assertions.assertThrows(com.altafjava.platform.core.exception.BusinessException.class,
				() -> FeeAssignment.forClassroom(1L, 3L).chargeIndividually(BigDecimal.TEN));
	}
}

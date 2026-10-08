package com.altafjava.school.domain.fee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.fee.model.DiscountType;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeDiscount;
import com.altafjava.school.domain.fee.model.FeeFrequency;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.InstallmentStatus.State;
import com.altafjava.school.domain.fee.service.FeeBalanceCalculator.Adjustments;

class FeeBalanceAdjustmentsTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

	private final FeeBalanceCalculator calculator = new FeeBalanceCalculator();

	private static BigDecimal money(String value) {
		return new BigDecimal(value);
	}

	private FeeStructure structure(String amount) {
		FeeStructure structure = FeeStructure.create("Tuition", money(amount), FeeFrequency.ANNUAL, null);
		structure.setId(1L);
		structure.configureLateFeePolicy(0, money("10"));
		return structure;
	}

	private FeeDiscount discount(DiscountType type, String value) {
		return FeeDiscount.grant(5L, 1L, type, money(value), "SCHOLARSHIP", "merit", 9L);
	}

	private FeeAssignment dueOn(LocalDate dueDate) {
		FeeAssignment assignment = FeeAssignment.forStudent(1L, 5L);
		assignment.configureDueDate(dueDate, null, null);
		return assignment;
	}

	@Test
	void percentageDiscount_reducesWhatIsDue() {
		FeeBalance balance = calculator.calculate(structure("1000.00"), null, BigDecimal.ZERO,
				new Adjustments(List.of(discount(DiscountType.PERCENTAGE, "25")), null, null), TODAY);

		assertEquals(0, money("250.00").compareTo(balance.discountAmount()));
		assertEquals(0, money("750.00").compareTo(balance.amountDue()));
		assertEquals(0, money("750.00").compareTo(balance.outstandingBalance()));
		assertEquals(0, money("1000.00").compareTo(balance.grossAmount()));
	}

	@Test
	void discounts_stackOnTheGrossAmount_andNeverExceedIt() {
		FeeBalance balance = calculator.calculate(structure("1000.00"), null, BigDecimal.ZERO,
				new Adjustments(List.of(discount(DiscountType.PERCENTAGE, "60"),
						discount(DiscountType.FIXED, "700")), null, null),
				TODAY);

		assertEquals(0, money("1000.00").compareTo(balance.discountAmount()));
		assertEquals(0, BigDecimal.ZERO.compareTo(balance.amountDue()));
	}

	@Test
	void revokedDiscount_hasNoEffect() {
		FeeDiscount revoked = discount(DiscountType.FIXED, "300");
		revoked.revoke("granted by mistake");

		FeeBalance balance = calculator.calculate(structure("1000.00"), null, BigDecimal.ZERO,
				new Adjustments(List.of(revoked), null, null), TODAY);

		assertEquals(0, BigDecimal.ZERO.compareTo(balance.discountAmount()));
		assertEquals(0, money("1000.00").compareTo(balance.amountDue()));
	}

	@Test
	void refund_reducesWhatIsConsideredPaid() {
		FeeBalance balance = calculator.calculate(structure("1000.00"), null, money("1000.00"),
				new Adjustments(null, money("400.00"), null), TODAY);

		assertEquals(0, money("600.00").compareTo(balance.amountPaid()));
		assertEquals(0, money("400.00").compareTo(balance.outstandingBalance()));
		assertEquals(0, money("400.00").compareTo(balance.refundedAmount()));
	}

	@Test
	void refundingAnOverpayment_leavesTheFeeSettled() {
		FeeBalance balance = calculator.calculate(structure("1000.00"), null, money("1200.00"),
				new Adjustments(null, money("200.00"), null), TODAY);

		assertEquals(0, BigDecimal.ZERO.compareTo(balance.outstandingBalance()));
		assertEquals(0, BigDecimal.ZERO.compareTo(balance.overpaidAmount()));
	}

	@Test
	void installments_spreadTheNetFeeAndAllocatePaymentsOldestFirst() {
		List<FeeInstallment> plan = InstallmentPlanFactory.evenSplit(5L, 1L, 4, TODAY.plusDays(30), 1);

		FeeBalance balance = calculator.calculate(structure("1000.00"), dueOn(TODAY.plusDays(400)),
				money("300.00"), new Adjustments(null, null, plan), TODAY);

		assertEquals(4, balance.installments().size());
		assertEquals(State.PAID, balance.installments().get(0).state());
		assertEquals(0, money("50.00").compareTo(balance.installments().get(1).paidAmount()));
		assertEquals(State.PARTIALLY_PAID, balance.installments().get(1).state());
		assertEquals(State.UPCOMING, balance.installments().get(2).state());
		assertEquals(TODAY.plusDays(30).plusMonths(1), balance.dueDate());
	}

	@Test
	void installments_followDiscountsBecauseTheyStoreSharesNotAmounts() {
		List<FeeInstallment> plan = InstallmentPlanFactory.evenSplit(5L, 1L, 2, TODAY.plusDays(30), 1);

		FeeBalance balance = calculator.calculate(structure("1000.00"), null, BigDecimal.ZERO,
				new Adjustments(List.of(discount(DiscountType.FIXED, "200")), null, plan), TODAY);

		assertEquals(0, money("400.00").compareTo(balance.installments().get(0).amount()));
		assertEquals(0, money("400.00").compareTo(balance.installments().get(1).amount()));
	}

	@Test
	void lateFee_appliesOnlyToTheOverdueInstallments() {
		List<FeeInstallment> plan = InstallmentPlanFactory.evenSplit(5L, 1L, 4, TODAY.minusDays(40), 1);

		FeeBalance balance = calculator.calculate(structure("1000.00"), null, BigDecimal.ZERO,
				new Adjustments(null, null, plan), TODAY);

		// Installments due 40 and 10 days ago are overdue (250 each); the other two are not.
		assertEquals(0, money("50.00").compareTo(balance.lateFeeAmount()));
		assertEquals(0, money("1050.00").compareTo(balance.outstandingBalance()));
		assertEquals(TODAY.minusDays(40), balance.dueDate());
		assertEquals(State.OVERDUE, balance.installments().get(0).state());
	}

	@Test
	void lateFee_vanishesOnceTheOverdueInstallmentsArePaid() {
		List<FeeInstallment> plan = InstallmentPlanFactory.evenSplit(5L, 1L, 4, TODAY.minusDays(40), 1);

		FeeBalance balance = calculator.calculate(structure("1000.00"), null, money("500.00"),
				new Adjustments(null, null, plan), TODAY);

		assertEquals(0, BigDecimal.ZERO.compareTo(balance.lateFeeAmount()));
		assertTrue(balance.installments().subList(0, 2).stream().allMatch(i -> i.state() == State.PAID));
	}

	@Test
	void withoutInstallments_behaviourIsUnchanged() {
		FeeBalance balance = calculator.calculate(structure("1000.00"), dueOn(TODAY.minusDays(5)),
				money("400.00"), TODAY);

		assertEquals(0, money("60.00").compareTo(balance.lateFeeAmount()));
		assertTrue(balance.installments().isEmpty());
	}
}

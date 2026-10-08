package com.altafjava.school.domain.fee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeeDiscount;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.InstallmentStatus;

// Computes the balance for a single FeeStructure already known to apply to the student — the
// selection of *which* structures apply (via FeeAssignment) happens in FeePaymentService.
public class FeeBalanceCalculator {

	private static final int DEFAULT_GRACE_DAYS = 0;
	private static final BigDecimal DEFAULT_LATE_FEE_PERCENTAGE = BigDecimal.ZERO;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	/** Everything that adjusts a structure's gross amount for one student. */
	public record Adjustments(List<FeeDiscount> discounts, BigDecimal refunded, List<FeeInstallment> installments) {

		public static final Adjustments NONE = new Adjustments(List.of(), BigDecimal.ZERO, List.of());

		public Adjustments {
			discounts = discounts == null ? List.of() : List.copyOf(discounts);
			refunded = refunded == null ? BigDecimal.ZERO : refunded;
			installments = installments == null ? List.of() : List.copyOf(installments);
		}
	}

	private final InstallmentScheduleCalculator scheduleCalculator = new InstallmentScheduleCalculator();

	public FeeBalance calculate(FeeStructure feeStructure, FeeAssignment assignment, BigDecimal totalPaid,
			LocalDate asOf) {
		return calculate(feeStructure, assignment, totalPaid, Adjustments.NONE, asOf);
	}

	/**
	 * @param assignment
	 *                       the resolved assignment this balance is computed for — nullable only for callers
	 *                       that genuinely have no assignment context (no due date is ever applied then, so no
	 *                       late fee either); prefer a non-null assignment wherever one is available.
	 * @param totalPaid
	 *                       gross of refunds; the completed refunds in {@code adjustments} are netted off here.
	 * @param asOf
	 *                       the date late-fee applicability is evaluated against — always the actual current
	 *                       date in production, an explicit parameter purely for deterministic testing.
	 */
	public FeeBalance calculate(FeeStructure feeStructure, FeeAssignment assignment, BigDecimal totalPaid,
			Adjustments adjustments, LocalDate asOf) {
		BigDecimal gross = feeStructure.getAmount();
		BigDecimal discount = adjustments.discounts().stream()
				.filter(FeeDiscount::isActive)
				.map(d -> d.amountOn(gross))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.min(gross);
		BigDecimal amountDue = gross.subtract(discount);
		BigDecimal paid = (totalPaid == null ? BigDecimal.ZERO : totalPaid).subtract(adjustments.refunded())
				.max(BigDecimal.ZERO);
		BigDecimal difference = amountDue.subtract(paid);
		BigDecimal baseOutstanding = difference.signum() > 0 ? difference : BigDecimal.ZERO;
		BigDecimal overpaid = difference.signum() < 0 ? difference.abs() : BigDecimal.ZERO;

		List<InstallmentStatus> installments = adjustments.installments().isEmpty() ? List.of()
				: scheduleCalculator.calculate(adjustments.installments(), amountDue, paid, asOf);
		LocalDate dueDate = installments.isEmpty()
				? (assignment != null ? assignment.getDueDate() : null)
				: installments.stream().filter(i -> i.outstandingAmount().signum() > 0)
						.map(InstallmentStatus::dueDate).findFirst().orElse(null);

		BigDecimal lateFee = installments.isEmpty()
				? lateFeeOnWhole(feeStructure, assignment, baseOutstanding, dueDate, asOf)
				: lateFeeOnInstallments(feeStructure, assignment, installments, asOf);

		return new FeeBalance(feeStructure.getId(), feeStructure.getName(), gross, discount, amountDue, paid,
				adjustments.refunded(), baseOutstanding.add(lateFee), overpaid, lateFee, dueDate, installments);
	}

	private BigDecimal lateFeeOnWhole(FeeStructure feeStructure, FeeAssignment assignment,
			BigDecimal baseOutstanding, LocalDate dueDate, LocalDate asOf) {
		if (dueDate == null || baseOutstanding.signum() <= 0) {
			return BigDecimal.ZERO;
		}
		return isPastGrace(feeStructure, assignment, dueDate, asOf)
				? percentageOf(baseOutstanding, feeStructure, assignment)
				: BigDecimal.ZERO;
	}

	// Each installment is late on its own clock: only what is overdue (past grace) attracts the fee.
	private BigDecimal lateFeeOnInstallments(FeeStructure feeStructure, FeeAssignment assignment,
			List<InstallmentStatus> installments, LocalDate asOf) {
		BigDecimal overdueOutstanding = installments.stream()
				.filter(i -> i.outstandingAmount().signum() > 0
						&& isPastGrace(feeStructure, assignment, i.dueDate(), asOf))
				.map(InstallmentStatus::outstandingAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return percentageOf(overdueOutstanding, feeStructure, assignment);
	}

	private boolean isPastGrace(FeeStructure feeStructure, FeeAssignment assignment, LocalDate dueDate,
			LocalDate asOf) {
		return asOf.isAfter(dueDate.plusDays(resolveGraceDays(assignment, feeStructure)));
	}

	private BigDecimal percentageOf(BigDecimal amount, FeeStructure feeStructure, FeeAssignment assignment) {
		return amount.multiply(resolveLateFeePercentage(assignment, feeStructure)).divide(ONE_HUNDRED, 2,
				RoundingMode.HALF_UP);
	}

	private int resolveGraceDays(FeeAssignment assignment, FeeStructure feeStructure) {
		if (assignment != null && assignment.getGraceDays() != null) {
			return assignment.getGraceDays();
		}
		if (feeStructure.getGraceDays() != null) {
			return feeStructure.getGraceDays();
		}
		return DEFAULT_GRACE_DAYS;
	}

	private BigDecimal resolveLateFeePercentage(FeeAssignment assignment, FeeStructure feeStructure) {
		if (assignment != null && assignment.getLateFeePercentage() != null) {
			return assignment.getLateFeePercentage();
		}
		if (feeStructure.getLateFeePercentage() != null) {
			return feeStructure.getLateFeePercentage();
		}
		return DEFAULT_LATE_FEE_PERCENTAGE;
	}
}

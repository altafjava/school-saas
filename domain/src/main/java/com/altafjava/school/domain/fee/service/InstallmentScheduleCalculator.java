package com.altafjava.school.domain.fee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.altafjava.school.domain.fee.model.FeeInstallment;
import com.altafjava.school.domain.fee.model.InstallmentStatus;
import com.altafjava.school.domain.fee.model.InstallmentStatus.State;

/**
 * Turns a plan's shares into amounts and allocates what has been paid across them, oldest due
 * date first. Amounts are rounded down to the cent except the last, which takes the remainder, so
 * they always add up to exactly the net fee.
 */
public class InstallmentScheduleCalculator {

	private static final BigDecimal TOTAL_BASIS_POINTS = BigDecimal.valueOf(FeeInstallment.TOTAL_BASIS_POINTS);

	public List<InstallmentStatus> calculate(List<FeeInstallment> plan, BigDecimal netAmount, BigDecimal paid,
			LocalDate asOf) {
		List<FeeInstallment> ordered = plan.stream()
				.sorted(Comparator.comparingInt(FeeInstallment::getSequenceNumber))
				.toList();
		BigDecimal remainingToAllocate = paid.max(BigDecimal.ZERO);
		BigDecimal allocatedAmount = BigDecimal.ZERO;
		List<InstallmentStatus> result = new ArrayList<>(ordered.size());
		for (int i = 0; i < ordered.size(); i++) {
			FeeInstallment installment = ordered.get(i);
			boolean last = i == ordered.size() - 1;
			BigDecimal amount = last ? netAmount.subtract(allocatedAmount)
					: netAmount.multiply(BigDecimal.valueOf(installment.getShareBasisPoints()))
							.divide(TOTAL_BASIS_POINTS, 2, RoundingMode.DOWN);
			allocatedAmount = allocatedAmount.add(amount);

			BigDecimal paidHere = remainingToAllocate.min(amount);
			remainingToAllocate = remainingToAllocate.subtract(paidHere);
			BigDecimal outstanding = amount.subtract(paidHere);
			result.add(new InstallmentStatus(installment.getSequenceNumber(), installment.getDueDate(), amount,
					paidHere, outstanding, stateOf(installment.getDueDate(), paidHere, outstanding, asOf)));
		}
		return result;
	}

	private State stateOf(LocalDate dueDate, BigDecimal paid, BigDecimal outstanding, LocalDate asOf) {
		if (outstanding.signum() == 0) {
			return State.PAID;
		}
		if (asOf.isAfter(dueDate)) {
			return State.OVERDUE;
		}
		return paid.signum() > 0 ? State.PARTIALLY_PAID : State.UPCOMING;
	}
}

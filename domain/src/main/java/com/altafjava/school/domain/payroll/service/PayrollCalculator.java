package com.altafjava.school.domain.payroll.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.payroll.model.PayrollComputation;
import com.altafjava.school.domain.payroll.model.SalarySnapshot;

/**
 * Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation, mirroring
 * {@code FeeBalanceCalculator}/{@code AttendancePercentageCalculator}.
 *
 * <p>
 * Loss-of-pay basis: {@code unpaidApprovedLeaveInMonth} must already be filtered by the caller to
 * {@code LeaveRequest}s with {@code status = APPROVED} whose {@code LeaveType.paid} is {@code false}
 * (see {@code PayslipService}) — this system has no employee attendance/absence tracking independent
 * of leave, so an explicit paid/unpaid flag on the leave type is the only honest signal available;
 * days requested against a paid leave type are never treated as loss-of-pay.
 */
public class PayrollCalculator {

	private static final int LOSS_OF_PAY_RATE_SCALE = 4;
	private static final int MONEY_SCALE = 2;

	public PayrollComputation compute(SalarySnapshot snapshot, YearMonth payMonth,
			List<LeaveRequest> unpaidApprovedLeaveInMonth) {
		BigDecimal grossPay = snapshot.grossPay();
		BigDecimal lossOfPayDays = lossOfPayDays(unpaidApprovedLeaveInMonth, payMonth);
		BigDecimal lossOfPayAmount = lossOfPayAmount(grossPay, lossOfPayDays, payMonth);
		BigDecimal netPay = grossPay.subtract(snapshot.totalDeductions()).subtract(lossOfPayAmount);
		return new PayrollComputation(grossPay, lossOfPayDays, lossOfPayAmount, netPay);
	}

	// Per-day rate is based on gross pay (basic + allowances), not basic pay alone — a day of
	// unpaid leave forfeits the full day's earned compensation, matching netPay = gross - deductions
	// - lossOfPayAmount.
	private BigDecimal lossOfPayAmount(BigDecimal grossPay, BigDecimal lossOfPayDays, YearMonth payMonth) {
		BigDecimal perDayRate = grossPay.divide(BigDecimal.valueOf(payMonth.lengthOfMonth()), LOSS_OF_PAY_RATE_SCALE,
				RoundingMode.HALF_UP);
		return perDayRate.multiply(lossOfPayDays).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal lossOfPayDays(List<LeaveRequest> unpaidApprovedLeaveInMonth, YearMonth payMonth) {
		LocalDate monthStart = payMonth.atDay(1);
		LocalDate monthEnd = payMonth.atEndOfMonth();
		BigDecimal total = BigDecimal.ZERO;
		for (LeaveRequest request : unpaidApprovedLeaveInMonth) {
			total = total.add(overlapDays(request, monthStart, monthEnd));
		}
		return total;
	}

	// A leave request can span a month boundary — clamp to the days that actually fall within the
	// target month rather than counting the request's full duration.
	private BigDecimal overlapDays(LeaveRequest request, LocalDate monthStart, LocalDate monthEnd) {
		LocalDate start = request.getStartDate().isAfter(monthStart) ? request.getStartDate() : monthStart;
		LocalDate end = request.getEndDate().isBefore(monthEnd) ? request.getEndDate() : monthEnd;
		if (end.isBefore(start)) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1);
	}
}

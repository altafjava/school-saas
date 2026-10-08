package com.altafjava.school.domain.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.model.PayrollComputation;
import com.altafjava.school.domain.payroll.model.SalarySnapshot;

class PayrollCalculatorTest {

	private final PayrollCalculator calculator = new PayrollCalculator();

	// basic 50000 + hra 10000 + transport 2000 + other allowances 500 = gross 62500; deductions 1000.
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

	private LeaveRequest unpaidLeave(LocalDate start, LocalDate end) {
		long inclusiveDays = ChronoUnit.DAYS.between(start, end) + 1;
		return LeaveRequest.submit(1L, 2L, 3L, start, end, "Personal", BigDecimal.valueOf(inclusiveDays), 1);
	}

	@Test
	void compute_withNoUnpaidLeave_zeroLossOfPay() {
		PayrollComputation computation = calculator.compute(snapshot(), YearMonth.of(2026, 6), List.of());

		assertEquals(0, BigDecimal.valueOf(62500).compareTo(computation.grossPay()));
		assertEquals(0, BigDecimal.ZERO.compareTo(computation.lossOfPayDays()));
		assertEquals(0, BigDecimal.ZERO.compareTo(computation.lossOfPayAmount()));
		assertEquals(0, BigDecimal.valueOf(61500).compareTo(computation.netPay()));
	}

	@Test
	void compute_withUnpaidLeaveWhollyWithinMonth_deductsFullDayCount() {
		// June has 30 days; 3 days of unpaid leave -> lossOfPayDays = 3.
		LeaveRequest leave = unpaidLeave(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12));

		PayrollComputation computation = calculator.compute(snapshot(), YearMonth.of(2026, 6), List.of(leave));

		assertEquals(0, BigDecimal.valueOf(3).compareTo(computation.lossOfPayDays()));
		BigDecimal expectedPerDay = BigDecimal.valueOf(62500).divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
		BigDecimal expectedLossOfPay = expectedPerDay.multiply(BigDecimal.valueOf(3)).setScale(2,
				RoundingMode.HALF_UP);
		assertEquals(0, expectedLossOfPay.compareTo(computation.lossOfPayAmount()));
		BigDecimal expectedNetPay = BigDecimal.valueOf(62500).subtract(BigDecimal.valueOf(1000))
				.subtract(expectedLossOfPay);
		assertEquals(0, expectedNetPay.compareTo(computation.netPay()));
	}

	@Test
	void compute_withLeaveSpanningMonthBoundary_clampsToDaysInsideTargetMonth() {
		// Only June 29-30 (2 days) fall inside June; July 1-2 must not be counted.
		LeaveRequest leave = unpaidLeave(LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 2));

		PayrollComputation computation = calculator.compute(snapshot(), YearMonth.of(2026, 6), List.of(leave));

		assertEquals(0, BigDecimal.valueOf(2).compareTo(computation.lossOfPayDays()));
	}

	@Test
	void compute_withMultipleUnpaidLeaveRequests_sumsAllOverlappingDays() {
		LeaveRequest first = unpaidLeave(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2));
		LeaveRequest second = unpaidLeave(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15));

		PayrollComputation computation = calculator.compute(snapshot(), YearMonth.of(2026, 6),
				List.of(first, second));

		assertEquals(0, BigDecimal.valueOf(3).compareTo(computation.lossOfPayDays()));
	}
}

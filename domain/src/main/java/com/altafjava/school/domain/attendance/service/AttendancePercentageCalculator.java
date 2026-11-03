package com.altafjava.school.domain.attendance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

// Pure domain logic (no Spring, no persistence) — kept unit/mutation-testable in isolation.
public class AttendancePercentageCalculator {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	/** Every marked day counts in the total; each status adds its own share of a day to the attended days. */
	public AttendancePercentage calculate(Map<AttendanceStatus, Long> daysByStatus) {
		long totalMarkedDays = 0;
		BigDecimal presentDays = BigDecimal.ZERO;
		for (Map.Entry<AttendanceStatus, Long> entry : daysByStatus.entrySet()) {
			long days = entry.getValue();
			if (days < 0) {
				throw new IllegalArgumentException("A day count must not be negative");
			}
			totalMarkedDays += days;
			presentDays = presentDays.add(entry.getKey().presenceCredit().multiply(BigDecimal.valueOf(days)));
		}
		if (totalMarkedDays == 0) {
			return new AttendancePercentage(BigDecimal.ZERO, 0, BigDecimal.ZERO);
		}
		BigDecimal percentage = presentDays.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(totalMarkedDays), 2, RoundingMode.HALF_UP);
		return new AttendancePercentage(presentDays.stripTrailingZeros(), totalMarkedDays, percentage);
	}
}

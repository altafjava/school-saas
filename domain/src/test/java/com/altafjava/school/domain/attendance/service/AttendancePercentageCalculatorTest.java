package com.altafjava.school.domain.attendance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

class AttendancePercentageCalculatorTest {

	private final AttendancePercentageCalculator calculator = new AttendancePercentageCalculator();

	@Test
	void calculate_everyDayPresent_isOneHundredPercent() {
		AttendancePercentage result = calculator.calculate(Map.of(AttendanceStatus.PRESENT, 10L));

		assertEquals(0, BigDecimal.TEN.compareTo(result.presentDays()));
		assertEquals(10, result.totalMarkedDays());
		assertEquals(0, new BigDecimal("100.00").compareTo(result.percentage()));
	}

	@Test
	void calculate_nothingMarked_isZeroWithoutDividingByZero() {
		AttendancePercentage result = calculator.calculate(Map.of());

		assertEquals(0, result.totalMarkedDays());
		assertEquals(0, BigDecimal.ZERO.compareTo(result.percentage()));
	}

	@Test
	void calculate_roundsHalfUpToTwoDecimals() {
		AttendancePercentage result = calculator
				.calculate(Map.of(AttendanceStatus.PRESENT, 1L, AttendanceStatus.ABSENT, 2L));

		assertEquals(0, new BigDecimal("33.33").compareTo(result.percentage()));
	}

	@Test
	void calculate_aLateArrival_countsAsPresent() {
		AttendancePercentage result = calculator
				.calculate(Map.of(AttendanceStatus.LATE, 1L, AttendanceStatus.ABSENT, 1L));

		assertEquals(0, BigDecimal.ONE.compareTo(result.presentDays()));
		assertEquals(0, new BigDecimal("50.00").compareTo(result.percentage()));
	}

	@Test
	void calculate_aHalfDay_countsAsHalfAPresentDay() {
		AttendancePercentage result = calculator
				.calculate(Map.of(AttendanceStatus.PRESENT, 3L, AttendanceStatus.HALF_DAY, 1L));

		assertEquals(0, new BigDecimal("3.5").compareTo(result.presentDays()));
		assertEquals(4, result.totalMarkedDays());
		assertEquals(0, new BigDecimal("87.50").compareTo(result.percentage()));
	}

	@Test
	void calculate_excusedAndAbsentDays_countOnlyInTheTotal() {
		AttendancePercentage result = calculator.calculate(
				Map.of(AttendanceStatus.PRESENT, 2L, AttendanceStatus.EXCUSED, 1L, AttendanceStatus.ABSENT, 1L));

		assertEquals(0, new BigDecimal("50.00").compareTo(result.percentage()));
	}

	@Test
	void calculate_negativeCount_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class, () -> calculator.calculate(Map.of(AttendanceStatus.PRESENT, -1L)));
	}
}

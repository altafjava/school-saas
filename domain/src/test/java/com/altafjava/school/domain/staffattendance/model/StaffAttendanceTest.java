package com.altafjava.school.domain.staffattendance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StaffAttendanceTest {

	private static final StaffWorkday WORKDAY = new StaffWorkday(LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 240);
	private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

	@Test
	void checkIn_withinTheGrace_isOnTime() {
		StaffAttendance day = StaffAttendance.checkIn(1L, DAY.atTime(9, 10), WORKDAY);

		assertFalse(day.isLate());
		assertEquals(StaffAttendanceStatus.PRESENT, day.getStatus());
	}

	@Test
	void checkIn_afterTheGrace_isLate() {
		assertTrue(StaffAttendance.checkIn(1L, DAY.atTime(9, 11), WORKDAY).isLate());
	}

	@Test
	void checkOut_beforeTheEnd_isEarly_andAShortDayIsAHalfDay() {
		StaffAttendance day = StaffAttendance.checkIn(1L, DAY.atTime(9, 0), WORKDAY);

		day.checkOut(DAY.atTime(12, 0), WORKDAY);

		assertTrue(day.isLeftEarly());
		assertEquals(StaffAttendanceStatus.HALF_DAY, day.getStatus());
		assertEquals(180, day.workedMinutes());
	}

	@Test
	void checkOut_afterAFullDay_staysPresent() {
		StaffAttendance day = StaffAttendance.checkIn(1L, DAY.atTime(9, 0), WORKDAY);

		day.checkOut(DAY.atTime(17, 5), WORKDAY);

		assertFalse(day.isLeftEarly());
		assertEquals(StaffAttendanceStatus.PRESENT, day.getStatus());
	}

	@Test
	void checkOut_twice_throwsBusinessException() {
		StaffAttendance day = StaffAttendance.checkIn(1L, DAY.atTime(9, 0), WORKDAY);
		day.checkOut(DAY.atTime(17, 0), WORKDAY);

		assertThrows(BusinessException.class, () -> day.checkOut(DAY.atTime(18, 0), WORKDAY));
	}

	@Test
	void recorded_absentWithATime_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> StaffAttendance.recorded(1L, DAY,
				new StaffAttendance.DayRecord(StaffAttendanceStatus.ABSENT, DAY.atTime(9, 0), null, null), WORKDAY,
				"hr"));
	}

	@Test
	void recorded_timesOnAnotherDay_throwsBusinessException() {
		LocalDateTime nextDay = DAY.plusDays(1).atTime(9, 0);

		assertThrows(BusinessException.class, () -> StaffAttendance.recorded(1L, DAY,
				new StaffAttendance.DayRecord(StaffAttendanceStatus.PRESENT, nextDay, null, null), WORKDAY, "hr"));
	}

	@Test
	void correct_recomputesLatenessAndHalfDayFromTheTimes() {
		StaffAttendance day = StaffAttendance.checkIn(1L, DAY.atTime(9, 30), WORKDAY);

		day.correct(new StaffAttendance.DayRecord(StaffAttendanceStatus.PRESENT, DAY.atTime(8, 55),
				DAY.atTime(10, 0), "forgot to check in"), WORKDAY, "hr");

		assertFalse(day.isLate());
		assertEquals(StaffAttendanceStatus.HALF_DAY, day.getStatus());
		assertEquals("hr", day.getMarkedBy());
	}

	@Test
	void workday_thatEndsBeforeItStarts_isRefused() {
		assertThrows(BusinessException.class, () -> new StaffWorkday(LocalTime.of(17, 0), LocalTime.of(9, 0), 0, 240));
	}
}

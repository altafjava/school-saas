package com.altafjava.school.domain.holiday.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class SchoolWeek {

	private SchoolWeek() {
	}

	// Sunday is off everywhere this product runs; a school's other days off are in its holiday calendar.
	public static boolean isWeeklyOff(LocalDate day) {
		return day.getDayOfWeek() == DayOfWeek.SUNDAY;
	}
}

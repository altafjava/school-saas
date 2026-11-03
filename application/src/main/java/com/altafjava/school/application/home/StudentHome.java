package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.util.List;

/** {@code timetable} is absent when the caller's role may not read it. */
public record StudentHome(LocalDate date, boolean schoolDay, List<TimetableSlot> timetable, StudentOverview overview,
		Inbox inbox) {

	public StudentHome {
		timetable = timetable == null ? null : List.copyOf(timetable);
	}
}

package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * The school's own "now". {@code academicYearId} and {@code yearStart} are absent when no academic
 * year is current; {@code holidays} covers the year so far.
 */
public record HomeDay(LocalDateTime now, boolean schoolDay, Long academicYearId, LocalDate yearStart,
		Set<LocalDate> holidays) {

	public HomeDay {
		holidays = Set.copyOf(holidays);
	}

	public LocalDate date() {
		return now.toLocalDate();
	}

	public boolean hasAcademicYear() {
		return academicYearId != null;
	}
}

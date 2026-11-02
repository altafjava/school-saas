package com.altafjava.school.application.home;

import java.time.LocalTime;
import com.altafjava.school.application.reference.ReferenceSummary;

/** One period of today's timetable, after substitutions. */
public record TimetableSlot(String periodName, LocalTime startTime, LocalTime endTime, ReferenceSummary classroom,
		ReferenceSummary subject, ReferenceSummary teacher, String venueName, SlotCoverage coverage) {
}

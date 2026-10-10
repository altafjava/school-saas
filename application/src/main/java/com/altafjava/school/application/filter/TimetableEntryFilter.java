package com.altafjava.school.application.filter;

import java.time.DayOfWeek;

/** Narrows the timetable-entry list; every part is optional. */
public record TimetableEntryFilter(String classroomPublicId, String teacherPublicId, String subjectPublicId,
		DayOfWeek dayOfWeek) {

	public static final TimetableEntryFilter NONE = new TimetableEntryFilter(null, null, null, null);
}

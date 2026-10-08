package com.altafjava.school.domain.timetable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

class TimetableEntryTest {

	@Test
	void create_setsAllFields() {
		TimetableEntry entry = TimetableEntry.create(DayOfWeek.MONDAY, 1L, 2L, 3L, 4L, null);

		assertEquals(DayOfWeek.MONDAY, entry.getDayOfWeek());
		assertEquals(1L, entry.getPeriodId());
		assertEquals(2L, entry.getClassroomId());
		assertEquals(3L, entry.getSubjectId());
		assertEquals(4L, entry.getTeacherId());
	}
}

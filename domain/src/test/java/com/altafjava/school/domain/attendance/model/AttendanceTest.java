package com.altafjava.school.domain.attendance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AttendanceTest {

	@Test
	void create_setsFields() {
		Attendance attendance = Attendance.create(1L, 2L, LocalDate.of(2026, 5, 1), AttendanceStatus.PRESENT,
				"teacher@school.test");

		assertEquals(1L, attendance.getStudentId());
		assertEquals(2L, attendance.getClassroomId());
		assertEquals(LocalDate.of(2026, 5, 1), attendance.getAttendanceDate());
		assertEquals(AttendanceStatus.PRESENT, attendance.getStatus());
		assertEquals("teacher@school.test", attendance.getMarkedBy());
	}

	@Test
	void updateStatus_changesStatus() {
		Attendance attendance = Attendance.create(1L, 2L, LocalDate.of(2026, 5, 1), AttendanceStatus.PRESENT,
				"teacher@school.test");

		attendance.updateStatus(AttendanceStatus.ABSENT);

		assertEquals(AttendanceStatus.ABSENT, attendance.getStatus());
	}
}

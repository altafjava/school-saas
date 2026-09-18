package com.altafjava.school.domain.attendance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AttendanceCorrectionTest {

	@Test
	void record_capturesOldAndNewStatus() {
		AttendanceCorrection correction = AttendanceCorrection.record(1L, AttendanceStatus.PRESENT,
				AttendanceStatus.ABSENT);

		assertEquals(1L, correction.getAttendanceId());
		assertEquals(AttendanceStatus.PRESENT, correction.getOldStatus());
		assertEquals(AttendanceStatus.ABSENT, correction.getNewStatus());
	}
}

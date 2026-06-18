package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AttendanceResponse(
		String publicId,
		Long studentId,
		Long classroomId,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}

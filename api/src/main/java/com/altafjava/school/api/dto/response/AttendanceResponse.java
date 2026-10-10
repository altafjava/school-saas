package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AttendanceResponse(
		String publicId,
		Long version,
		String studentPublicId,
		String classroomPublicId,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}

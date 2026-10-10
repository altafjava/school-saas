package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record PeriodAttendanceResponse(
		String publicId,
		String studentPublicId,
		String classroomPublicId,
		String timetableEntryPublicId,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}

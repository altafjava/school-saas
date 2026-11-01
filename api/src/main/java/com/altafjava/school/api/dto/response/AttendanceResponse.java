package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record AttendanceResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String classroomPublicId,
		ReferenceSummary classroom,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}

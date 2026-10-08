package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AttendanceScanResponse(
		String attendancePublicId,
		String studentPublicId,
		String studentName,
		String className,
		LocalDate attendanceDate,
		String status,
		boolean alreadyMarked) {
}

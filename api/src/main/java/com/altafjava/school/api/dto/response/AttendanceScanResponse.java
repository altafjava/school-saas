package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;

public record AttendanceScanResponse(
		@NoLabel("Reply to a single write whose caller just sent this id") String attendancePublicId,
		String studentPublicId,
		String studentName,
		String className,
		LocalDate attendanceDate,
		String status,
		boolean alreadyMarked) {
}

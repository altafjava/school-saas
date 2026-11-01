package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record PeriodAttendanceResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String classroomPublicId,
		ReferenceSummary classroom,
		@NoLabel("Has no human name") String timetableEntryPublicId,
		LocalDate attendanceDate,
		String status,
		String markedBy) {
}

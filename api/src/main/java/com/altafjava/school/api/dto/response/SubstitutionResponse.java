package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record SubstitutionResponse(
		String publicId,
		LocalDate date,
		boolean active,
		String timetableEntryPublicId,
		String dayOfWeek,
		Long periodId,
		Long classroomId,
		Long subjectId,
		String regularTeacherPublicId,
		String regularTeacherName,
		String substituteTeacherPublicId,
		String substituteTeacherName,
		String reason,
		Instant cancelledAt,
		String cancellationReason) {
}

package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record SubstitutionResponse(
		String publicId,
		LocalDate date,
		boolean active,
		@NoLabel("Has no human name") String timetableEntryPublicId,
		String dayOfWeek,
		String periodPublicId,
		ReferenceSummary period,
		String classroomPublicId,
		ReferenceSummary classroom,
		String subjectPublicId,
		ReferenceSummary subject,
		String regularTeacherPublicId,
		String regularTeacherName,
		String substituteTeacherPublicId,
		String substituteTeacherName,
		String reason,
		Instant cancelledAt,
		String cancellationReason) {
}

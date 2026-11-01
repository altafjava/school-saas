package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record TimetableEntryResponse(
		String publicId,
		Long version,
		String dayOfWeek,
		String periodPublicId,
		ReferenceSummary period,
		String classroomPublicId,
		ReferenceSummary classroom,
		String subjectPublicId,
		ReferenceSummary subject,
		String teacherPublicId,
		ReferenceSummary teacher,
		String venuePublicId,
		ReferenceSummary venue) {
}

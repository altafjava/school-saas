package com.altafjava.school.api.dto.response;

public record TimetableEntryResponse(
		String publicId,
		Long version,
		String dayOfWeek,
		String periodPublicId,
		String classroomPublicId,
		String subjectPublicId,
		String teacherPublicId,
		String venuePublicId) {
}

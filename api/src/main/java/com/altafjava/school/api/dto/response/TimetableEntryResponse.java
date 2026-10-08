package com.altafjava.school.api.dto.response;

public record TimetableEntryResponse(
		String publicId,
		String dayOfWeek,
		Long periodId,
		Long classroomId,
		Long subjectId,
		Long teacherId,
		Long venueId) {
}

package com.altafjava.school.api.dto.response;

/** The classroom a student sits in for the current academic year. */
public record CurrentClassroomResponse(
		String publicId,
		String name,
		String rollNumber,
		String academicYearPublicId) {
}

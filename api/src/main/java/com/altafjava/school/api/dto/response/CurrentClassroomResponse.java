package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

/** The classroom a student sits in for the current academic year. */
public record CurrentClassroomResponse(
		String publicId,
		String name,
		String rollNumber,
		@NoLabel("Read through the record it belongs to") String academicYearPublicId) {
}

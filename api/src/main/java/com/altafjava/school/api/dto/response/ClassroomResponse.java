package com.altafjava.school.api.dto.response;

public record ClassroomResponse(
		String publicId,
		String classCode,
		String grade,
		String section,
		String academicYear,
		Long classTeacherId) {
}

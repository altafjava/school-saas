package com.altafjava.school.api.dto.response;

public record ClassroomResponse(
		String publicId,
		Long version,
		String classCode,
		String grade,
		String section,
		String academicYear,
		String classTeacherPublicId,
		String curriculumPublicId,
		Integer capacity) {
}

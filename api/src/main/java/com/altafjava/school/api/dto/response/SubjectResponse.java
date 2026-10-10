package com.altafjava.school.api.dto.response;

public record SubjectResponse(
		String publicId,
		String code,
		String name,
		String description,
		boolean active,
		String curriculumPublicId) {
}

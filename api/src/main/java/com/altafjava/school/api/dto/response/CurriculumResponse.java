package com.altafjava.school.api.dto.response;

public record CurriculumResponse(
		String publicId,
		String boardPublicId,
		String name,
		String code,
		String description,
		String gradingScalePublicId,
		boolean active) {
}

package com.altafjava.school.api.dto.response;

public record ExamTypeDefinitionResponse(
		String publicId,
		Long version,
		String code,
		String name,
		int displayOrder,
		boolean active) {
}

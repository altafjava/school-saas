package com.altafjava.school.api.dto.response;

public record BookCopyResponse(
		String publicId,
		Long version,
		String bookPublicId,
		String copyCode,
		String status) {
}

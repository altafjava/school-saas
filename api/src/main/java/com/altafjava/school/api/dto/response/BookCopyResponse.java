package com.altafjava.school.api.dto.response;

public record BookCopyResponse(
		String publicId,
		String bookPublicId,
		String copyCode,
		String status) {
}

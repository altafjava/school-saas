package com.altafjava.school.api.dto.response;

public record EventRegistrationResponse(
		String publicId,
		String eventPublicId,
		String studentPublicId,
		String status) {
}

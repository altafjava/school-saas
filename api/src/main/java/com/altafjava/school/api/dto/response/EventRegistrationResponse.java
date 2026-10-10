package com.altafjava.school.api.dto.response;

public record EventRegistrationResponse(
		String publicId,
		Long version,
		String eventPublicId,
		String studentPublicId,
		String status) {
}

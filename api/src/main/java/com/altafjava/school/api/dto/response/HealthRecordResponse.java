package com.altafjava.school.api.dto.response;

public record HealthRecordResponse(
		String publicId,
		Long version,
		String studentPublicId,
		String bloodGroup,
		String allergies,
		String conditions,
		String immunizations) {
}

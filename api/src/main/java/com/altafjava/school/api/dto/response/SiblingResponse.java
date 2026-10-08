package com.altafjava.school.api.dto.response;

public record SiblingResponse(
		String publicId,
		String studentCode,
		String firstName,
		String lastName,
		String enrollmentStatus,
		String photoFilePublicId) {
}

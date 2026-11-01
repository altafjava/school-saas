package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

public record SiblingResponse(
		String publicId,
		Long version,
		String studentCode,
		String firstName,
		String lastName,
		String enrollmentStatus,
		@NoLabel("Has no human name") String photoFilePublicId) {
}

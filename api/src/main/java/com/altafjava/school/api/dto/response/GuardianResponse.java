package com.altafjava.school.api.dto.response;

public record GuardianResponse(
		String publicId,
		Long version,
		String firstName,
		String lastName,
		String email,
		String phone,
		AddressResponse address,
		String photoFilePublicId) {
}

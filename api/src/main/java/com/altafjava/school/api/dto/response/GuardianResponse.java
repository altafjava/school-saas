package com.altafjava.school.api.dto.response;

public record GuardianResponse(
		String publicId,
		String firstName,
		String lastName,
		String email,
		String phone,
		AddressResponse address,
		String photoFilePublicId) {
}

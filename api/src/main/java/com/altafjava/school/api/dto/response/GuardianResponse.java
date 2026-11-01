package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

public record GuardianResponse(
		String publicId,
		Long version,
		String firstName,
		String lastName,
		String email,
		String phone,
		AddressResponse address,
		@NoLabel("Has no human name") String photoFilePublicId) {
}

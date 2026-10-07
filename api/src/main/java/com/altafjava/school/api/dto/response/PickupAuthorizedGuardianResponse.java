package com.altafjava.school.api.dto.response;

// Carries what a gate/front-desk check needs to confirm the person: name, phone, photo.
public record PickupAuthorizedGuardianResponse(
		String guardianPublicId,
		String firstName,
		String lastName,
		String phone,
		String photoFilePublicId,
		String relationshipType) {
}

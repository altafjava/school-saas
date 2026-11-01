package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

// Carries what a gate/front-desk check needs to confirm the person: name, phone, photo.
public record PickupAuthorizedGuardianResponse(
		@NoLabel("The row already carries this person's name") String guardianPublicId,
		String firstName,
		String lastName,
		String phone,
		@NoLabel("Has no human name") String photoFilePublicId,
		String relationshipType) {
}

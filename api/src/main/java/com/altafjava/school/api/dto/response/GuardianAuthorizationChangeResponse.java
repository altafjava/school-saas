package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record GuardianAuthorizationChangeResponse(
		String publicId,
		Long version,
		String guardianPublicId,
		boolean oldAuthorizedForPickup,
		boolean newAuthorizedForPickup,
		boolean oldCustodyRestricted,
		boolean newCustodyRestricted,
		String note,
		String changedByUserPublicId,
		Instant changedAt) {
}

package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record GuardianAuthorizationChangeResponse(
		String publicId,
		Long guardianId,
		boolean oldAuthorizedForPickup,
		boolean newAuthorizedForPickup,
		boolean oldCustodyRestricted,
		boolean newCustodyRestricted,
		String note,
		Long changedByUserId,
		Instant changedAt) {
}

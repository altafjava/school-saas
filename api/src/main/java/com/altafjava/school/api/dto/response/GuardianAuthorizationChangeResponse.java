package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record GuardianAuthorizationChangeResponse(
		String publicId,
		Long version,
		String guardianPublicId,
		ReferenceSummary guardian,
		boolean oldAuthorizedForPickup,
		boolean newAuthorizedForPickup,
		boolean oldCustodyRestricted,
		boolean newCustodyRestricted,
		String note,
		String changedByUserPublicId,
		ReferenceSummary changedByUser,
		Instant changedAt) {
}

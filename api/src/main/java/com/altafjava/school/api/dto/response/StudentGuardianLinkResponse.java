package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record StudentGuardianLinkResponse(
		String publicId,
		Long studentId,
		Long guardianId,
		String relationshipType,
		boolean primaryContact,
		Instant consentGivenAt,
		boolean authorizedForPickup,
		boolean custodyRestricted,
		String custodyRestrictionNote) {
}

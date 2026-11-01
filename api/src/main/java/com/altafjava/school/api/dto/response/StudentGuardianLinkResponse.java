package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record StudentGuardianLinkResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String guardianPublicId,
		ReferenceSummary guardian,
		String relationshipType,
		boolean primaryContact,
		Instant consentGivenAt,
		boolean authorizedForPickup,
		boolean custodyRestricted,
		String custodyRestrictionNote) {
}

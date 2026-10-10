package com.altafjava.school.api.dto.response;

import java.time.Instant;

/** A guardian as seen from one student: the person plus what ties them to this student. */
public record StudentGuardianResponse(
		String linkPublicId,
		Long linkVersion,
		String guardianPublicId,
		String firstName,
		String lastName,
		String email,
		String phone,
		String relationshipType,
		boolean primaryContact,
		boolean authorizedForPickup,
		boolean custodyRestricted,
		String custodyRestrictionNote,
		Instant consentGivenAt) {
}

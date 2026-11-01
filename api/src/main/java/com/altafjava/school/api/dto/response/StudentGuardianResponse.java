package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.api.support.NoLabel;

/** A guardian as seen from one student: the person plus what ties them to this student. */
public record StudentGuardianResponse(
		@NoLabel("Has no human name") String linkPublicId,
		Long linkVersion,
		@NoLabel("The row already carries this person's name") String guardianPublicId,
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

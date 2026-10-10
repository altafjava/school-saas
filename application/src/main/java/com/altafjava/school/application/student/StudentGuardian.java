package com.altafjava.school.application.student;

import java.time.Instant;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;

/** A guardian as seen from one student: the person, plus what ties them to that student. */
public record StudentGuardian(
		String linkPublicId,
		Long linkVersion,
		String guardianPublicId,
		String firstName,
		String lastName,
		String email,
		String phone,
		RelationshipType relationshipType,
		boolean primaryContact,
		boolean authorizedForPickup,
		boolean custodyRestricted,
		String custodyRestrictionNote,
		Instant consentGivenAt) {

	public static StudentGuardian of(StudentGuardianLink link, Guardian guardian) {
		return new StudentGuardian(link.getPublicId().toString(), link.getVersion(),
				guardian.getPublicId().toString(), guardian.getFirstName(), guardian.getLastName(),
				guardian.getEmail(), guardian.getPhone(), link.getRelationshipType(), link.isPrimaryContact(),
				link.isAuthorizedForPickup(), link.isCustodyRestricted(), link.getCustodyRestrictionNote(),
				link.getConsentGivenAt());
	}
}

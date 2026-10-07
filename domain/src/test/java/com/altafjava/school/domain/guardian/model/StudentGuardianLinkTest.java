package com.altafjava.school.domain.guardian.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StudentGuardianLinkTest {

	@Test
	void create_setsFields() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.MOTHER, true);

		assertEquals(1L, link.getStudentId());
		assertEquals(2L, link.getGuardianId());
		assertEquals(RelationshipType.MOTHER, link.getRelationshipType());
		assertTrue(link.isPrimaryContact());
		assertNull(link.getConsentGivenAt());
	}

	@Test
	void giveConsent_setsConsentTimestamp() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.FATHER, false);

		link.giveConsent();

		assertNotNull(link.getConsentGivenAt());
	}

	@Test
	void revokeConsent_clearsConsentTimestamp() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.FATHER, false);
		link.giveConsent();

		link.revokeConsent();

		assertNull(link.getConsentGivenAt());
	}

	@Test
	void newLink_isNotAuthorizedForPickup() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.MOTHER, true);

		assertEquals(PickupDecision.NOT_AUTHORIZED, link.pickupDecision());
	}

	@Test
	void authorizePickup_makesLinkAuthorized() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.OTHER, false);

		link.authorizePickup();

		assertEquals(PickupDecision.AUTHORIZED, link.pickupDecision());
	}

	@Test
	void restrictCustody_revokesPickupAndWinsOverIt() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.FATHER, false);
		link.authorizePickup();

		link.restrictCustody("court order 2026/114");

		assertTrue(link.isCustodyRestricted());
		assertFalse(link.isAuthorizedForPickup());
		assertEquals("court order 2026/114", link.getCustodyRestrictionNote());
		assertEquals(PickupDecision.CUSTODY_RESTRICTED, link.pickupDecision());
	}

	@Test
	void authorizePickup_whileCustodyRestricted_throws() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.FATHER, false);
		link.restrictCustody("court order");

		assertThrows(BusinessException.class, link::authorizePickup);
		assertFalse(link.isAuthorizedForPickup());
	}

	@Test
	void liftCustodyRestriction_doesNotRestorePickup() {
		StudentGuardianLink link = StudentGuardianLink.create(1L, 2L, RelationshipType.FATHER, false);
		link.authorizePickup();
		link.restrictCustody("court order");

		link.liftCustodyRestriction();

		assertFalse(link.isCustodyRestricted());
		assertNull(link.getCustodyRestrictionNote());
		assertEquals(PickupDecision.NOT_AUTHORIZED, link.pickupDecision());
	}
}

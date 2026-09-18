package com.altafjava.school.domain.guardian.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

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
}

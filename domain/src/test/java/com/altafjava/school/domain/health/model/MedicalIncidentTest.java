package com.altafjava.school.domain.health.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MedicalIncidentTest {

	@Test
	void record_setsFieldsAndDefaultsGuardianNotifiedFalse() {
		LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 1, 10, 30);

		MedicalIncident incident = MedicalIncident.record(1L, occurredAt, "Fell during PE", "Ice pack applied", 99L);

		assertEquals(1L, incident.getStudentId());
		assertEquals(occurredAt, incident.getOccurredAt());
		assertEquals("Fell during PE", incident.getDescription());
		assertEquals("Ice pack applied", incident.getTreatmentGiven());
		assertEquals(99L, incident.getRecordedByUserId());
		assertFalse(incident.isGuardianNotified());
	}

	@Test
	void markGuardianNotified_setsFlagTrue() {
		MedicalIncident incident = MedicalIncident.record(1L, LocalDateTime.of(2026, 5, 1, 10, 30), "Fell during PE",
				"Ice pack applied", 99L);

		incident.markGuardianNotified();

		assertTrue(incident.isGuardianNotified());
	}

	@Test
	void erasePii_tombstonesDescriptionAndClearsTreatment() {
		MedicalIncident incident = MedicalIncident.record(1L, LocalDateTime.of(2026, 5, 1, 10, 30), "Fell during PE",
				"Ice pack applied", 99L);

		incident.erasePii();

		assertEquals("[erased]", incident.getDescription());
		assertNull(incident.getTreatmentGiven());
		assertEquals(1L, incident.getStudentId(), "studentId must survive erasure to keep the row addressable");
	}
}

package com.altafjava.school.domain.discipline.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DisciplineIncidentTest {

	@Test
	void report_startsWithGuardianNotNotified() {
		DisciplineIncident incident = DisciplineIncident.report(1L, 2L, LocalDate.of(2026, 5, 1),
				IncidentSeverity.MINOR, "Talking during class");

		assertFalse(incident.isGuardianNotified());
		assertEquals(IncidentSeverity.MINOR, incident.getSeverity());
	}

	@Test
	void recordAction_setsActionTaken() {
		DisciplineIncident incident = DisciplineIncident.report(1L, 2L, LocalDate.of(2026, 5, 1),
				IncidentSeverity.MAJOR, "Fighting");

		incident.recordAction("Suspended for one day");

		assertEquals("Suspended for one day", incident.getActionTaken());
	}

	@Test
	void markGuardianNotified_flipsFlag() {
		DisciplineIncident incident = DisciplineIncident.report(1L, 2L, LocalDate.of(2026, 5, 1),
				IncidentSeverity.SEVERE, "Vandalism");

		incident.markGuardianNotified();

		assertTrue(incident.isGuardianNotified());
	}

	@Test
	void erasePii_tombstonesDescriptionAndClearsActionTaken() {
		DisciplineIncident incident = DisciplineIncident.report(1L, 2L, LocalDate.of(2026, 5, 1),
				IncidentSeverity.MAJOR, "Fighting");
		incident.recordAction("Suspended for one day");

		incident.erasePii();

		assertEquals("[erased]", incident.getDescription());
		assertNull(incident.getActionTaken());
		assertEquals(1L, incident.getStudentId(), "studentId must survive erasure to keep the row addressable");
	}
}

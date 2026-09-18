package com.altafjava.school.domain.counseling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CounselingSessionTest {

	@Test
	void schedule_setsFields() {
		CounselingSession session = CounselingSession.schedule(1L, 2L, LocalDate.of(2026, 5, 1),
				"Discussed exam anxiety", true);

		assertEquals(1L, session.getStudentId());
		assertEquals(2L, session.getCounselorTeacherId());
		assertEquals(LocalDate.of(2026, 5, 1), session.getSessionDate());
		assertEquals("Discussed exam anxiety", session.getNotes());
		assertTrue(session.isFollowUpRequired());
	}

	@Test
	void updateNotes_changesNotesAndFollowUpFlag() {
		CounselingSession session = CounselingSession.schedule(1L, 2L, LocalDate.of(2026, 5, 1), "Initial notes",
				true);

		session.updateNotes("Follow-up complete, no further action", false);

		assertEquals("Follow-up complete, no further action", session.getNotes());
		assertFalse(session.isFollowUpRequired());
	}

	@Test
	void erasePii_clearsNotes() {
		CounselingSession session = CounselingSession.schedule(1L, 2L, LocalDate.of(2026, 5, 1),
				"Discussed exam anxiety", true);

		session.erasePii();

		assertNull(session.getNotes());
		assertEquals(1L, session.getStudentId(), "studentId must survive erasure to keep the row addressable");
	}
}

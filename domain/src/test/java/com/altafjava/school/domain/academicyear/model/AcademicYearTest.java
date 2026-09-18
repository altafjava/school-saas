package com.altafjava.school.domain.academicyear.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AcademicYearTest {

	@Test
	void create_setsFields() {
		AcademicYear academicYear = AcademicYear.create("2025-26", LocalDate.of(2025, 4, 1),
				LocalDate.of(2026, 3, 31), true);

		assertEquals("2025-26", academicYear.getName());
		assertEquals(LocalDate.of(2025, 4, 1), academicYear.getStartDate());
		assertEquals(LocalDate.of(2026, 3, 31), academicYear.getEndDate());
		assertTrue(academicYear.isCurrent());
	}

	@Test
	void markNotCurrent_clearsCurrentFlag() {
		AcademicYear academicYear = AcademicYear.create("2025-26", LocalDate.of(2025, 4, 1),
				LocalDate.of(2026, 3, 31), true);

		academicYear.markNotCurrent();

		assertFalse(academicYear.isCurrent());
	}
}

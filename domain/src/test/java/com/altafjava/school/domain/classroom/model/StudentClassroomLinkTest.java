package com.altafjava.school.domain.classroom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StudentClassroomLinkTest {

	@Test
	void create_setsFields() {
		StudentClassroomLink link = StudentClassroomLink.create(1L, 2L, 3L, LocalDate.of(2026, 6, 1));

		assertEquals(1L, link.getStudentId());
		assertEquals(2L, link.getClassroomId());
		assertEquals(3L, link.getAcademicYearId());
		assertEquals(LocalDate.of(2026, 6, 1), link.getEnrolledAt());
	}

	@Test
	void softDelete_marksLinkDeleted() {
		StudentClassroomLink link = StudentClassroomLink.create(1L, 2L, 3L, LocalDate.of(2026, 6, 1));
		assertFalse(link.isDeleted());

		link.softDelete("withdrawal");

		assertTrue(link.isDeleted());
	}
}

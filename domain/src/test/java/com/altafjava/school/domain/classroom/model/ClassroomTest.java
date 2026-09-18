package com.altafjava.school.domain.classroom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class ClassroomTest {

	@Test
	void create_setsFields() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2025-26", 20L);

		assertEquals("CLS-1", classroom.getClassCode());
		assertEquals("Grade 5", classroom.getGrade());
		assertEquals("A", classroom.getSection());
		assertEquals(10L, classroom.getAcademicYearId());
		assertEquals("2025-26", classroom.getAcademicYear());
		assertEquals(20L, classroom.getClassTeacherId());
		assertNull(classroom.getCapacity());
	}

	@Test
	void updateCapacity_setsCapacity() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2025-26", 20L);

		classroom.updateCapacity(30);

		assertEquals(30, classroom.getCapacity());
	}

	@Test
	void reassignTeacher_changesClassTeacherId() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2025-26", 20L);

		classroom.reassignTeacher(99L);

		assertEquals(99L, classroom.getClassTeacherId());
	}

	@Test
	void reassignAcademicYear_changesYearIdAndName() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2025-26", 20L);

		classroom.reassignAcademicYear(11L, "2026-27");

		assertEquals(11L, classroom.getAcademicYearId());
		assertEquals("2026-27", classroom.getAcademicYear());
	}

	@Test
	void assignCurriculum_setsCurriculumId() {
		Classroom classroom = Classroom.create("CLS-1", "Grade 5", "A", 10L, "2025-26", 20L);

		classroom.assignCurriculum(5L);

		assertEquals(5L, classroom.getCurriculumId());
	}
}

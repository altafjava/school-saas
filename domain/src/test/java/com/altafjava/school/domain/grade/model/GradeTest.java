package com.altafjava.school.domain.grade.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GradeTest {

	@Test
	void create_setsFields() {
		Grade grade = Grade.create(1L, 2L, 3L, BigDecimal.valueOf(88), "B+", "teacher@school.test");

		assertEquals(1L, grade.getStudentId());
		assertEquals(2L, grade.getSubjectId());
		assertEquals(3L, grade.getExamId());
		assertEquals(BigDecimal.valueOf(88), grade.getMarks());
		assertEquals("B+", grade.getGradeLetter());
		assertEquals("teacher@school.test", grade.getGradedBy());
	}

	@Test
	void correct_updatesMarksAndLetter() {
		Grade grade = Grade.create(1L, 2L, 3L, BigDecimal.valueOf(88), "B+", "teacher@school.test");

		grade.correct(BigDecimal.valueOf(92), "A-");

		assertEquals(BigDecimal.valueOf(92), grade.getMarks());
		assertEquals("A-", grade.getGradeLetter());
	}
}

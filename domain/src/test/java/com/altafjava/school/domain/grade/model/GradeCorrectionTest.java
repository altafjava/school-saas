package com.altafjava.school.domain.grade.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GradeCorrectionTest {

	@Test
	void record_capturesOldAndNewMarksAndLetter() {
		GradeCorrection correction = GradeCorrection.record(1L, BigDecimal.valueOf(88), "B+",
				BigDecimal.valueOf(92), "A-");

		assertEquals(1L, correction.getGradeId());
		assertEquals(BigDecimal.valueOf(88), correction.getOldMarks());
		assertEquals("B+", correction.getOldGradeLetter());
		assertEquals(BigDecimal.valueOf(92), correction.getNewMarks());
		assertEquals("A-", correction.getNewGradeLetter());
	}
}

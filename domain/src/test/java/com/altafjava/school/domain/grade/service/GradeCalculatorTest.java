package com.altafjava.school.domain.grade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.curriculum.model.GradeBand;

class GradeCalculatorTest {

	private final GradeCalculator calculator = new GradeCalculator();

	private List<GradeBand> defaultThresholds() {
		return List.of(
				new GradeBand("A", new BigDecimal("90"), new BigDecimal("4.0")),
				new GradeBand("B", new BigDecimal("80"), new BigDecimal("3.0")),
				new GradeBand("C", new BigDecimal("70"), new BigDecimal("2.0")),
				new GradeBand("D", new BigDecimal("60"), new BigDecimal("1.0")),
				new GradeBand("F", BigDecimal.ZERO, BigDecimal.ZERO));
	}

	@Test
	void calculateLetterGrade_atExactBoundary_awardsHigherGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(90), BigDecimal.valueOf(100),
				defaultThresholds());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_justBelowBoundary_awardsLowerGrade() {
		String grade = calculator.calculateLetterGrade(new BigDecimal("89.99"), BigDecimal.valueOf(100),
				defaultThresholds());

		assertEquals("B", grade);
	}

	@Test
	void calculateLetterGrade_zeroMarks_awardsLowestGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.ZERO, BigDecimal.valueOf(100),
				defaultThresholds());

		assertEquals("F", grade);
	}

	@Test
	void calculateLetterGrade_fullMarks_awardsTopGrade() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(100), BigDecimal.valueOf(100),
				defaultThresholds());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_nonStandardMaxMarks_computesPercentageCorrectly() {
		String grade = calculator.calculateLetterGrade(BigDecimal.valueOf(45), BigDecimal.valueOf(50),
				defaultThresholds());

		assertEquals("A", grade);
	}

	@Test
	void calculateLetterGrade_marksAboveMaxMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(101), BigDecimal.valueOf(100),
						defaultThresholds()));
	}

	@Test
	void calculateLetterGrade_negativeMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(-1), BigDecimal.valueOf(100),
						defaultThresholds()));
	}

	@Test
	void calculateLetterGrade_zeroMaxMarks_throwsIllegalArgument() {
		assertThrows(IllegalArgumentException.class,
				() -> calculator.calculateLetterGrade(BigDecimal.valueOf(10), BigDecimal.ZERO,
						defaultThresholds()));
	}

	@Test
	void resolvePoints_matchingLetter_returnsItsPoints() {
		var points = calculator.resolvePoints("B", defaultThresholds());

		assertTrue(points.isPresent());
		assertEquals(0, new BigDecimal("3.0").compareTo(points.get()));
	}

	@Test
	void resolvePoints_unmatchedLetter_returnsEmpty() {
		var points = calculator.resolvePoints("Z", defaultThresholds());

		assertTrue(points.isEmpty());
	}
}

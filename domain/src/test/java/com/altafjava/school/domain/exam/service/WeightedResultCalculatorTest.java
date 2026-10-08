package com.altafjava.school.domain.exam.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedResultCalculatorTest {

	private final WeightedResultCalculator calculator = new WeightedResultCalculator();

	private ExamScore score(String marks, String maxMarks, String weightage) {
		return new ExamScore(new BigDecimal(marks), new BigDecimal(maxMarks), new BigDecimal(weightage));
	}

	@Test
	void percentage_noScores_returnsEmpty() {
		assertTrue(calculator.percentage(List.of()).isEmpty());
	}

	@Test
	void percentage_singleExam_isThatExamsPercentage() {
		var result = calculator.percentage(List.of(score("45", "50", "100")));

		assertEquals(0, new BigDecimal("90.0000").compareTo(result.get()));
	}

	@Test
	void percentage_weightsExamsByTheirWeightage() {
		var result = calculator.percentage(List.of(score("18", "20", "30"), score("60", "100", "70")));

		// (90 * 30 + 60 * 70) / 100
		assertEquals(0, new BigDecimal("69.0000").compareTo(result.get()));
	}

	@Test
	void percentage_isIndependentOfEachExamsMaxMarks() {
		var small = calculator.percentage(List.of(score("5", "10", "50"), score("50", "100", "50")));
		var large = calculator.percentage(List.of(score("50", "100", "50"), score("500", "1000", "50")));

		assertEquals(small.get(), large.get());
	}

	@Test
	void percentage_partialTermWeights_areRenormalisedOverTheExamsSat() {
		var result = calculator.percentage(List.of(score("80", "100", "20")));

		assertEquals(0, new BigDecimal("80.0000").compareTo(result.get()));
	}
}

package com.altafjava.school.domain.grade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.school.domain.grade.service.GpaCalculator.WeightedPoints;

class GpaCalculatorTest {

	private final GpaCalculator calculator = new GpaCalculator();

	private WeightedPoints points(String points, String weightage) {
		return new WeightedPoints(new BigDecimal(points), new BigDecimal(weightage));
	}

	@Test
	void calculateWeightedAverage_emptyList_returnsEmpty() {
		assertTrue(calculator.calculateWeightedAverage(List.of()).isEmpty());
	}

	@Test
	void calculateWeightedAverage_singleEntry_returnsThatPoint() {
		var average = calculator.calculateWeightedAverage(List.of(points("4.0", "30")));

		assertEquals(0, new BigDecimal("4.00").compareTo(average.get()));
	}

	@Test
	void calculateWeightedAverage_equalWeights_returnsRoundedMean() {
		var average = calculator.calculateWeightedAverage(
				List.of(points("4.0", "100"), points("3.0", "100"), points("3.0", "100")));

		assertEquals(0, new BigDecimal("3.33").compareTo(average.get()));
	}

	@Test
	void calculateWeightedAverage_unequalWeights_favoursTheHeavierExam() {
		var average = calculator.calculateWeightedAverage(List.of(points("4.0", "20"), points("2.0", "80")));

		assertEquals(0, new BigDecimal("2.40").compareTo(average.get()));
	}
}

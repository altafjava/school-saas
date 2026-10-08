package com.altafjava.school.domain.exam.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

// Pure domain logic (no Spring, no persistence). Weights are relative to the exams the student
// actually sat, so an exam they were excused from neither helps nor hurts the result.
public class WeightedResultCalculator {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final int SCALE = 4;

	public Optional<BigDecimal> percentage(List<ExamScore> scores) {
		BigDecimal totalWeight = scores.stream().map(ExamScore::weightage).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalWeight.signum() <= 0) {
			return Optional.empty();
		}
		BigDecimal weightedSum = scores.stream()
				.map(score -> percentageOf(score).multiply(score.weightage()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return Optional.of(weightedSum.divide(totalWeight, SCALE, RoundingMode.HALF_UP));
	}

	private BigDecimal percentageOf(ExamScore score) {
		return score.marks().multiply(ONE_HUNDRED).divide(score.maxMarks(), 6, RoundingMode.HALF_UP);
	}
}

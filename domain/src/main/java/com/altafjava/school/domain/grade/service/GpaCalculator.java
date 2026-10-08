package com.altafjava.school.domain.grade.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

// Pure domain logic (no Spring, no persistence) — average of resolved grade points, each weighted
// by its exam's weightage.
public class GpaCalculator {

	public record WeightedPoints(BigDecimal points, BigDecimal weightage) {
	}

	public Optional<BigDecimal> calculateWeightedAverage(List<WeightedPoints> weightedPoints) {
		BigDecimal totalWeight = weightedPoints.stream().map(WeightedPoints::weightage).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		if (totalWeight.signum() <= 0) {
			return Optional.empty();
		}
		BigDecimal weightedSum = weightedPoints.stream()
				.map(entry -> entry.points().multiply(entry.weightage()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return Optional.of(weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP));
	}
}

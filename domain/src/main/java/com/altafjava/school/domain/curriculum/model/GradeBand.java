package com.altafjava.school.domain.curriculum.model;

import java.math.BigDecimal;

/** One step of a grading scale, small and immutable so a classroom's scale can be cached. */
public record GradeBand(String letter, BigDecimal minPercentage, BigDecimal points) {

	public static GradeBand of(GradingScaleThreshold threshold) {
		return new GradeBand(threshold.getLetter(), threshold.getMinPercentage(), threshold.getPoints());
	}
}

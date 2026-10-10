package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record GradingScaleResponse(
		String publicId,
		Long version,
		String name,
		boolean isDefault,
		boolean active,
		List<Threshold> thresholds) {

	public GradingScaleResponse {
		thresholds = thresholds == null ? List.of() : List.copyOf(thresholds);
	}

	public record Threshold(String letter, BigDecimal minPercentage, BigDecimal points) {
	}
}

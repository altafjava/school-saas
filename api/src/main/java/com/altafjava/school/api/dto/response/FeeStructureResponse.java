package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record FeeStructureResponse(
		String publicId,
		Long version,
		String name,
		BigDecimal amount,
		String frequency,
		String planType,
		Integer graceDays,
		BigDecimal lateFeePercentage) {
}

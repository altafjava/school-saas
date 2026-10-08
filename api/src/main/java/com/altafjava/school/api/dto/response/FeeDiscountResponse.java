package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeDiscountResponse(
		String publicId,
		Long feeStructureId,
		String discountType,
		BigDecimal discountValue,
		String category,
		String reason,
		boolean active,
		Instant grantedAt,
		Long grantedByUserId,
		Instant revokedAt,
		String revocationReason) {
}

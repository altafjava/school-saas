package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeDiscountResponse(
		String publicId,
		Long version,
		String feeStructurePublicId,
		String discountType,
		BigDecimal discountValue,
		String category,
		String reason,
		boolean active,
		Instant grantedAt,
		String grantedByUserPublicId,
		Instant revokedAt,
		String revocationReason) {
}

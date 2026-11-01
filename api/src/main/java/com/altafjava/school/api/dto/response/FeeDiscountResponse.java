package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record FeeDiscountResponse(
		String publicId,
		Long version,
		String feeStructurePublicId,
		ReferenceSummary feeStructure,
		String discountType,
		BigDecimal discountValue,
		String category,
		String reason,
		boolean active,
		Instant grantedAt,
		String grantedByUserPublicId,
		ReferenceSummary grantedByUser,
		Instant revokedAt,
		String revocationReason) {
}

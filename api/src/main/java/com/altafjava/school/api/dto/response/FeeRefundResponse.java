package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeRefundResponse(
		String publicId,
		Long version,
		BigDecimal amount,
		String reason,
		String method,
		String status,
		String creditNoteNumber,
		String gatewayRefundReference,
		String failureReason,
		Instant refundedAt,
		String refundedByUserPublicId) {
}

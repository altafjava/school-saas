package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeRefundResponse(
		String publicId,
		BigDecimal amount,
		String reason,
		String method,
		String status,
		String creditNoteNumber,
		String gatewayRefundReference,
		String failureReason,
		Instant refundedAt,
		Long refundedByUserId) {
}

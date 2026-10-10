package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FeePaymentResponse(
		String publicId,
		String studentPublicId,
		String feeStructurePublicId,
		BigDecimal paidAmount,
		LocalDateTime paidAt,
		String receiptNumber) {
}

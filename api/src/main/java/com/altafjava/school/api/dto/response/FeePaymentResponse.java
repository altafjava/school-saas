package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.fee.model.PaymentSource;

public record FeePaymentResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String feeStructurePublicId,
		ReferenceSummary feeStructure,
		BigDecimal paidAmount,
		LocalDateTime paidAt,
		String receiptNumber,
		PaymentSource paymentSource,
		String gatewayProviderType) {
}

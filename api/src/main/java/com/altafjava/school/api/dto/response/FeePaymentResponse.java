package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.fee.model.FeePaymentStatus;
import com.altafjava.school.domain.fee.model.PaymentMode;
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
		String gatewayProviderType,
		PaymentMode paymentMode,
		String transactionReference,
		String chequeNumber,
		LocalDate chequeDate,
		String chequeBank,
		FeePaymentStatus status,
		LocalDateTime bouncedAt,
		String bounceReason) {
}

package com.altafjava.school.api.dto.response.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.fee.model.FeeInvoiceStatus;
import com.altafjava.school.domain.fee.model.InvoiceSettlement;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A fee invoice for one student and period. Paid and outstanding come from cleared payments made "
		+ "against it; a bounced cheque no longer counts.")
public record FeeInvoiceResponse(String publicId, Long version, String invoiceNumber, String studentPublicId,
		ReferenceSummary student, LocalDate periodStart, LocalDate periodEnd, LocalDate issueDate, LocalDate dueDate,
		List<Line> lines, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal outstandingAmount,
		InvoiceSettlement.State settlement, FeeInvoiceStatus status, Instant cancelledAt, String cancellationReason) {

	public FeeInvoiceResponse {
		lines = lines == null ? null : List.copyOf(lines);
	}

	@Schema(description = "One charge. The installment number is absent for a whole fee.")
	public record Line(String feeStructurePublicId, ReferenceSummary feeStructure, Integer installmentNumber,
			String description, BigDecimal amount) {
	}
}

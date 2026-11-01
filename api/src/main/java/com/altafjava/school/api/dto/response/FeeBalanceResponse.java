package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;

public record FeeBalanceResponse(
		String feeStructurePublicId,
		ReferenceSummary feeStructure,
		String feeStructureName,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal amountDue,
		BigDecimal amountPaid,
		BigDecimal refundedAmount,
		BigDecimal outstandingBalance,
		BigDecimal overpaidAmount,
		BigDecimal lateFeeAmount,
		LocalDate dueDate,
		List<InstallmentStatusResponse> installments) {
}

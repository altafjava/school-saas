package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FeeBalanceResponse(
		Long feeStructureId,
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

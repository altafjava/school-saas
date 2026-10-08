package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentStatusResponse(
		int sequenceNumber,
		LocalDate dueDate,
		BigDecimal amount,
		BigDecimal paidAmount,
		BigDecimal outstandingAmount,
		String state) {
}

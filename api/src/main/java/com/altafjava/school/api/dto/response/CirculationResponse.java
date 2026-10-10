package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CirculationResponse(
		String publicId,
		Long version,
		String bookCopyPublicId,
		String studentPublicId,
		LocalDate checkedOutAt,
		LocalDate dueDate,
		LocalDate returnedAt,
		BigDecimal fineAmount,
		int renewalCount,
		LocalDate lastRenewedAt) {
}

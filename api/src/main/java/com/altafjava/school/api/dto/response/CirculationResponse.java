package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record CirculationResponse(
		String publicId,
		Long version,
		String bookCopyPublicId,
		ReferenceSummary bookCopy,
		String studentPublicId,
		ReferenceSummary student,
		LocalDate checkedOutAt,
		LocalDate dueDate,
		LocalDate returnedAt,
		BigDecimal fineAmount,
		int renewalCount,
		LocalDate lastRenewedAt) {
}

package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record BookReservationResponse(
		String publicId,
		Long version,
		String bookPublicId,
		ReferenceSummary book,
		String studentPublicId,
		ReferenceSummary student,
		String status,
		Instant reservedAt,
		String heldCopyPublicId,
		ReferenceSummary heldCopy,
		LocalDate holdExpiresOn,
		Instant closedAt) {
}

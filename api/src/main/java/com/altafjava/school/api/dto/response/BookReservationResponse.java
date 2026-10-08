package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record BookReservationResponse(
		String publicId,
		Long bookId,
		Long studentId,
		String status,
		Instant reservedAt,
		Long heldCopyId,
		LocalDate holdExpiresOn,
		Instant closedAt) {
}

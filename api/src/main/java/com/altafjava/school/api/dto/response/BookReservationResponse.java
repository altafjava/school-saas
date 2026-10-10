package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record BookReservationResponse(
		String publicId,
		String bookPublicId,
		String studentPublicId,
		String status,
		Instant reservedAt,
		String heldCopyPublicId,
		LocalDate holdExpiresOn,
		Instant closedAt) {
}

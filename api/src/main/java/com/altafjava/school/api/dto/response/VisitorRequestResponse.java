package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record VisitorRequestResponse(
		String publicId,
		String visitorName,
		String visitorPhone,
		String purpose,
		Long hostEmployeeId,
		LocalDate visitDate,
		String source,
		String status,
		String photoFilePublicId,
		Long decidedByUserId,
		Instant decidedAt,
		String decisionReason) {
}

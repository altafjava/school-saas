package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record VisitorRequestResponse(
		String publicId,
		Long version,
		String visitorName,
		String visitorPhone,
		String purpose,
		String hostEmployeePublicId,
		LocalDate visitDate,
		String source,
		String status,
		String photoFilePublicId,
		String decidedByUserPublicId,
		Instant decidedAt,
		String decisionReason) {
}

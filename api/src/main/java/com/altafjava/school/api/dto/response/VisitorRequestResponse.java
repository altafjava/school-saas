package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record VisitorRequestResponse(
		String publicId,
		Long version,
		String visitorName,
		String visitorPhone,
		String purpose,
		String hostEmployeePublicId,
		ReferenceSummary hostEmployee,
		LocalDate visitDate,
		String source,
		String status,
		@NoLabel("Has no human name") String photoFilePublicId,
		String decidedByUserPublicId,
		ReferenceSummary decidedByUser,
		Instant decidedAt,
		String decisionReason) {
}

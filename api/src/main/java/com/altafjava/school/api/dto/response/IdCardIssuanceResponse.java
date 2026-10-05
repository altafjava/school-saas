package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record IdCardIssuanceResponse(
		String publicId,
		String documentType,
		String verificationCode,
		Instant issuedAt) {
}

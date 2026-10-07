package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record IssuedDocumentResponse(
		String publicId,
		String documentType,
		String title,
		String verificationCode,
		Instant issuedAt,
		boolean revoked,
		Instant revokedAt,
		String revocationReason) {
}

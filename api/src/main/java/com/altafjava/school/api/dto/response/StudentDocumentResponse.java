package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record StudentDocumentResponse(
		String publicId,
		String studentPublicId,
		String documentType,
		String filePublicId,
		String verificationStatus,
		Instant verifiedAt,
		String rejectionReason) {
}

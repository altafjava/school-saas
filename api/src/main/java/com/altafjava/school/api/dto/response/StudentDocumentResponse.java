package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.api.support.NoLabel;

public record StudentDocumentResponse(
		String publicId,
		@NoLabel("Read through the record it belongs to") String studentPublicId,
		String documentType,
		@NoLabel("Has no human name") String filePublicId,
		String verificationStatus,
		Instant verifiedAt,
		String rejectionReason) {
}

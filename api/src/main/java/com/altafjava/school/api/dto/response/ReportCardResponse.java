package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record ReportCardResponse(String publicId,
		Long version, String termPublicId, Instant generatedAt, String teacherRemarks,
		String principalRemarks) {
}

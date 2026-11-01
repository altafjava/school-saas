package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record ReportCardResponse(String publicId,
		Long version, String termPublicId,
		ReferenceSummary term, Instant generatedAt, String teacherRemarks,
		String principalRemarks) {
}

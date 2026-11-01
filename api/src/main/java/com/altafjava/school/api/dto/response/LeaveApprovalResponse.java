package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record LeaveApprovalResponse(
		String publicId,
		Long version,
		String stage,
		String decision,
		String decidedByUserPublicId,
		ReferenceSummary decidedByUser,
		Instant decidedAt,
		String remarks) {
}

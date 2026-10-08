package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record LeaveApprovalResponse(
		String publicId,
		String stage,
		String decision,
		Long decidedByUserId,
		Instant decidedAt,
		String remarks) {
}

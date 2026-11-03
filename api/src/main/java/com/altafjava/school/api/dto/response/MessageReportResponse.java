package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record MessageReportResponse(
		String publicId,
		@NoLabel("A thread has no name of its own") String conversationPublicId,
		@NoLabel("A message has no name of its own") String messagePublicId,
		String reportedByUserPublicId,
		ReferenceSummary reportedByUser,
		String reason,
		String status,
		String resolvedByUserPublicId,
		ReferenceSummary resolvedByUser,
		String resolutionNote,
		Instant resolvedAt,
		Instant reportedAt) {
}

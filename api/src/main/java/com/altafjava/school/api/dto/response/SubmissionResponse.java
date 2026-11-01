package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record SubmissionResponse(
		String publicId,
		Long version,
		String assignmentPublicId,
		ReferenceSummary assignment,
		String studentPublicId,
		ReferenceSummary student,
		LocalDateTime submittedAt,
		String storageKey,
		String textContent,
		String status,
		BigDecimal marksObtained,
		String feedback,
		LocalDateTime gradedAt,
		Long gradedBy) {
}

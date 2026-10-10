package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmissionResponse(
		String publicId,
		String assignmentPublicId,
		String studentPublicId,
		LocalDateTime submittedAt,
		String storageKey,
		String textContent,
		String status,
		BigDecimal marksObtained,
		String feedback,
		LocalDateTime gradedAt,
		Long gradedBy) {
}

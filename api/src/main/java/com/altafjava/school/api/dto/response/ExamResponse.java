package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ExamResponse(
		String publicId,
		Long version,
		String title,
		String subjectPublicId,
		String classroomPublicId,
		LocalDateTime scheduledAt,
		BigDecimal maxMarks,
		String termPublicId,
		String status,
		String examTypePublicId,
		BigDecimal weightage,
		boolean resultsPublished,
		Instant resultsPublishedAt) {
}

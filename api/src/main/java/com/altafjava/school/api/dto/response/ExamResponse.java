package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record ExamResponse(
		String publicId,
		Long version,
		String title,
		String subjectPublicId,
		ReferenceSummary subject,
		String classroomPublicId,
		ReferenceSummary classroom,
		LocalDateTime scheduledAt,
		BigDecimal maxMarks,
		String termPublicId,
		ReferenceSummary term,
		String status,
		String examTypePublicId,
		ReferenceSummary examType,
		BigDecimal weightage,
		boolean resultsPublished,
		Instant resultsPublishedAt) {
}

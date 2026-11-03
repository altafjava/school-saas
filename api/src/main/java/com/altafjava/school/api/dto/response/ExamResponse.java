package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.exam.model.MarksStage;

public record ExamResponse(
		String publicId,
		Long version,
		String title,
		String subjectPublicId,
		ReferenceSummary subject,
		String classroomPublicId,
		ReferenceSummary classroom,
		LocalDateTime scheduledAt,
		int durationMinutes,
		LocalDateTime endsAt,
		String venuePublicId,
		ReferenceSummary venue,
		boolean schedulePublished,
		Instant schedulePublishedAt,
		BigDecimal maxMarks,
		String termPublicId,
		ReferenceSummary term,
		String status,
		String examTypePublicId,
		ReferenceSummary examType,
		BigDecimal weightage,
		boolean resultsPublished,
		Instant resultsPublishedAt,
		MarksStage marksStage,
		Instant marksVerifiedAt,
		Instant marksLockedAt) {
}

package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamResponse(
		String publicId,
		String title,
		String subject,
		Long classroomId,
		LocalDateTime scheduledAt,
		BigDecimal maxMarks) {
}

package com.altafjava.school.domain.grade.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One student's mark on an exam whose results are published. */
public record PublishedResult(Long studentId, UUID examPublicId, String examTitle, Long subjectId, BigDecimal marks,
		BigDecimal maxMarks, String gradeLetter, Instant publishedAt) {
}

package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import com.altafjava.school.application.reference.ReferenceSummary;

public record GradeResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String subjectPublicId,
		ReferenceSummary subject,
		String examPublicId,
		ReferenceSummary exam,
		BigDecimal marks,
		String gradeLetter,
		String gradedBy) {
}

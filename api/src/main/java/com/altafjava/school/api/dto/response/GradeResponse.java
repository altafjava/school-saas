package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record GradeResponse(
		String publicId,
		String studentPublicId,
		String subjectPublicId,
		String examPublicId,
		BigDecimal marks,
		String gradeLetter,
		String gradedBy) {
}

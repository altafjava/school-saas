package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record GradeCorrectionResponse(
		String publicId,
		Long version,
		BigDecimal oldMarks,
		String oldGradeLetter,
		BigDecimal newMarks,
		String newGradeLetter,
		String correctedBy,
		Instant correctedAt) {
}

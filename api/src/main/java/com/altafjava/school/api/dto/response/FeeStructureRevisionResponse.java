package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeStructureRevisionResponse(
		String publicId,
		Long version,
		BigDecimal oldAmount,
		BigDecimal newAmount,
		String revisedBy,
		Instant revisedAt) {
}

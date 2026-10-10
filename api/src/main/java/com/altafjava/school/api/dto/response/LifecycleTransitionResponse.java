package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record LifecycleTransitionResponse(
		String publicId,
		Long version,
		String fromStage,
		String toStage,
		String reason,
		LocalDate effectiveOn,
		Instant recordedAt,
		String recordedByUserPublicId) {
}

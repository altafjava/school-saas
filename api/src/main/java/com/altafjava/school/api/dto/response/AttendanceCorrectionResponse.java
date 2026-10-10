package com.altafjava.school.api.dto.response;

import java.time.Instant;

public record AttendanceCorrectionResponse(
		String publicId,
		Long version,
		String oldStatus,
		String newStatus,
		String correctedBy,
		Instant correctedAt) {
}

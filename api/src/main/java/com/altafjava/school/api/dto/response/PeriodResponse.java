package com.altafjava.school.api.dto.response;

import java.time.LocalTime;

public record PeriodResponse(
		String publicId,
		Long version,
		String name,
		LocalTime startTime,
		LocalTime endTime,
		int displayOrder) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AcademicYearResponse(
		String publicId,
		String name,
		LocalDate startDate,
		LocalDate endDate,
		boolean current) {
}

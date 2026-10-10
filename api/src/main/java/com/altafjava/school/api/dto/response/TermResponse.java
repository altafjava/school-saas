package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TermResponse(
		String publicId,
		Long version,
		String name,
		LocalDate startDate,
		LocalDate endDate,
		String academicYearPublicId,
		boolean current) {
}

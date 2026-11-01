package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record TermResponse(
		String publicId,
		Long version,
		String name,
		LocalDate startDate,
		LocalDate endDate,
		String academicYearPublicId,
		ReferenceSummary academicYear,
		boolean current) {
}

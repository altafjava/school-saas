package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record HolidayResponse(
		String publicId,
		Long version,
		LocalDate date,
		String name,
		boolean recurring) {
}

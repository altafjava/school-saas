package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record AttendancePercentageResponse(
		BigDecimal presentDays,
		long totalMarkedDays,
		BigDecimal percentage) {
}

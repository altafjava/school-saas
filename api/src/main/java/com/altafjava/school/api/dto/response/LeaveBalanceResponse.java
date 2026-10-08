package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
		String publicId,
		Long employeeId,
		Long leaveTypeId,
		Long academicYearId,
		BigDecimal allocatedDays,
		BigDecimal usedDays,
		BigDecimal remainingDays) {
}

package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
		String publicId,
		String employeePublicId,
		String leaveTypePublicId,
		String academicYearPublicId,
		BigDecimal allocatedDays,
		BigDecimal usedDays,
		BigDecimal remainingDays) {
}

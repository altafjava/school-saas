package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import com.altafjava.school.application.reference.ReferenceSummary;

public record LeaveBalanceResponse(
		String publicId,
		Long version,
		String employeePublicId,
		ReferenceSummary employee,
		String leaveTypePublicId,
		ReferenceSummary leaveType,
		String academicYearPublicId,
		ReferenceSummary academicYear,
		BigDecimal allocatedDays,
		BigDecimal usedDays,
		BigDecimal remainingDays) {
}

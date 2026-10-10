package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;

public record LeaveTypeResponse(
		String publicId,
		Long version,
		String name,
		BigDecimal defaultAnnualDays,
		boolean active,
		boolean paid,
		boolean availableDuringProbation,
		boolean carryForwardEnabled,
		BigDecimal maxCarryForwardDays,
		Integer carryForwardExpiryMonths,
		int approvalLevels) {
}

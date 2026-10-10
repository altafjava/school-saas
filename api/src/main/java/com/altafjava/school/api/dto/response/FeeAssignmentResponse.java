package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeAssignmentResponse(
		String publicId,
		String feeStructurePublicId,
		String scope,
		String studentPublicId,
		String classroomPublicId,
		LocalDate dueDate,
		Integer graceDays,
		BigDecimal lateFeePercentage) {
}

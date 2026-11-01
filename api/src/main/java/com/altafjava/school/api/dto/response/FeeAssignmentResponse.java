package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record FeeAssignmentResponse(
		String publicId,
		Long version,
		String feeStructurePublicId,
		ReferenceSummary feeStructure,
		String scope,
		String studentPublicId,
		ReferenceSummary student,
		String classroomPublicId,
		ReferenceSummary classroom,
		LocalDate dueDate,
		Integer graceDays,
		BigDecimal lateFeePercentage) {
}

package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
		String publicId,
		String employeePublicId,
		String leaveTypePublicId,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal daysRequested,
		String reason,
		String status,
		String approvedByUserPublicId,
		LocalDateTime approvedAt,
		String rejectionReason,
		int approvalsRequired,
		int approvalsGranted,
		String awaitingStage) {
}

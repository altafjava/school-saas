package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
		String publicId,
		Long employeeId,
		Long leaveTypeId,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal daysRequested,
		String reason,
		String status,
		Long approvedByUserId,
		LocalDateTime approvedAt,
		String rejectionReason,
		int approvalsRequired,
		int approvalsGranted,
		String awaitingStage) {
}

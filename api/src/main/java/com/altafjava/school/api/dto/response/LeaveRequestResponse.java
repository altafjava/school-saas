package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record LeaveRequestResponse(
		String publicId,
		Long version,
		String employeePublicId,
		ReferenceSummary employee,
		String leaveTypePublicId,
		ReferenceSummary leaveType,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal daysRequested,
		String reason,
		String status,
		String approvedByUserPublicId,
		ReferenceSummary approvedByUser,
		LocalDateTime approvedAt,
		String rejectionReason,
		int approvalsRequired,
		int approvalsGranted,
		String awaitingStage) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record StudentLeaveRequestResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String classroomPublicId,
		ReferenceSummary classroom,
		LocalDate startDate,
		LocalDate endDate,
		String reason,
		@NoLabel("Has no human name") String attachmentFilePublicId,
		String status,
		String decidedByUserPublicId,
		ReferenceSummary decidedByUser,
		LocalDateTime decidedAt,
		String rejectionReason) {
}

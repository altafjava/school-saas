package com.altafjava.school.api.dto.response;

import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.student.model.EnrollmentStatus;

public record PromotionPreviewResponse(
		String sourceClassroomPublicId,
		ReferenceSummary sourceClassroom,
		String targetClassroomPublicId,
		ReferenceSummary targetClassroom,
		Integer seatsLeft,
		List<PromotionPreviewRow> students) {

	public PromotionPreviewResponse {
		students = List.copyOf(students);
	}

	public record PromotionPreviewRow(
			String studentPublicId,
			ReferenceSummary student,
			EnrollmentStatus enrollmentStatus,
			boolean alreadyPlaced,
			String blockedReason) {
	}
}

package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.classroom.model.PromotionOutcome;

public record PromotionResultResponse(
		String studentPublicId,
		ReferenceSummary student,
		PromotionOutcome outcome,
		String classroomPublicId,
		ReferenceSummary classroom) {
}

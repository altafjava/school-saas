package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record CurriculumResponse(
		String publicId,
		Long version,
		String boardPublicId,
		ReferenceSummary board,
		String name,
		String code,
		String description,
		String gradingScalePublicId,
		ReferenceSummary gradingScale,
		boolean active) {
}

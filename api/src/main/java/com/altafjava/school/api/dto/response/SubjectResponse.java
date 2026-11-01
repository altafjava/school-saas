package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record SubjectResponse(
		String publicId,
		Long version,
		String code,
		String name,
		String description,
		boolean active,
		String curriculumPublicId,
		ReferenceSummary curriculum) {
}

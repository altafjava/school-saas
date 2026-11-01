package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record HealthRecordResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String bloodGroup,
		String allergies,
		String conditions,
		String immunizations) {
}

package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record EventRegistrationResponse(
		String publicId,
		Long version,
		String eventPublicId,
		ReferenceSummary event,
		String studentPublicId,
		ReferenceSummary student,
		String status) {
}

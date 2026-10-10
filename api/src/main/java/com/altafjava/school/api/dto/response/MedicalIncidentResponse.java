package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record MedicalIncidentResponse(
		String publicId,
		String studentPublicId,
		LocalDateTime occurredAt,
		String description,
		String treatmentGiven,
		boolean guardianNotified,
		String recordedByUserPublicId) {
}

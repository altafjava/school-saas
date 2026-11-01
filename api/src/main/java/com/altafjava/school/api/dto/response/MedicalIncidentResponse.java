package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record MedicalIncidentResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		LocalDateTime occurredAt,
		String description,
		String treatmentGiven,
		boolean guardianNotified,
		String recordedByUserPublicId,
		ReferenceSummary recordedByUser) {
}

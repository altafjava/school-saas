package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record DisciplineIncidentResponse(
		String publicId,
		String studentPublicId,
		String reportedByTeacherPublicId,
		LocalDate incidentDate,
		String severity,
		String description,
		String actionTaken,
		boolean guardianNotified) {
}

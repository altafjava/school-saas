package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record DisciplineIncidentResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String reportedByTeacherPublicId,
		ReferenceSummary reportedByTeacher,
		LocalDate incidentDate,
		String severity,
		String description,
		String actionTaken,
		boolean guardianNotified) {
}

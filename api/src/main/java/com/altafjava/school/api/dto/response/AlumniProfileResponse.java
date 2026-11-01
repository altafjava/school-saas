package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record AlumniProfileResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		int graduationYear,
		String currentOccupation,
		String contactEmail,
		String contactPhone,
		boolean active) {
}

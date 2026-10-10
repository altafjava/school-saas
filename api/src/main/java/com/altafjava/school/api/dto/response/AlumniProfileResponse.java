package com.altafjava.school.api.dto.response;

public record AlumniProfileResponse(
		String publicId,
		String studentPublicId,
		int graduationYear,
		String currentOccupation,
		String contactEmail,
		String contactPhone,
		boolean active) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record AssetAssignmentResponse(
		String publicId,
		Long version,
		String assetPublicId,
		String assignedToType,
		String assignedToPublicId,
		LocalDate assignedAt,
		LocalDate returnedAt) {
}

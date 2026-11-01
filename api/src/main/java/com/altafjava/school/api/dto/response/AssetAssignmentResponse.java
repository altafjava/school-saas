package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record AssetAssignmentResponse(
		String publicId,
		Long version,
		String assetPublicId,
		ReferenceSummary asset,
		String assignedToType,
		@NoLabel("Points at a student or an employee, chosen by assignedToType") String assignedToPublicId,
		LocalDate assignedAt,
		LocalDate returnedAt) {
}

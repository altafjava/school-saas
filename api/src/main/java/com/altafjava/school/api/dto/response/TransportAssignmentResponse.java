package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record TransportAssignmentResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String routePublicId,
		ReferenceSummary route,
		String vehiclePublicId,
		ReferenceSummary vehicle,
		String routeStopPublicId,
		ReferenceSummary routeStop,
		LocalDate effectiveFrom,
		LocalDate effectiveTo) {
}

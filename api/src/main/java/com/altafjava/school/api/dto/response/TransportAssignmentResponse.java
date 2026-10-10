package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TransportAssignmentResponse(
		String publicId,
		String studentPublicId,
		String routePublicId,
		String vehiclePublicId,
		String routeStopPublicId,
		LocalDate effectiveFrom,
		LocalDate effectiveTo) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record RouteStopResponse(
		String publicId,
		Long version,
		String routePublicId,
		ReferenceSummary route,
		String stopName,
		int sequenceOrder,
		LocalTime pickupTime,
		LocalTime dropTime) {
}

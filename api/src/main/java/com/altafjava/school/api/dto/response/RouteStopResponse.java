package com.altafjava.school.api.dto.response;

import java.time.LocalTime;

public record RouteStopResponse(
		String publicId,
		String routePublicId,
		String stopName,
		int sequenceOrder,
		LocalTime pickupTime,
		LocalTime dropTime) {
}

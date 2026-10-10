package com.altafjava.school.api.dto.response;

public record VenueResponse(
		String publicId,
		Long version,
		String code,
		String name,
		String venueType,
		Integer capacity,
		boolean active) {
}

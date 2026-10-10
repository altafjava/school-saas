package com.altafjava.school.api.dto.response;

public record RouteResponse(
		String publicId,
		Long version,
		String name,
		String code,
		String description,
		boolean active) {
}

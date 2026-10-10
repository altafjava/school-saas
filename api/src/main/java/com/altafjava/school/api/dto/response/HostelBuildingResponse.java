package com.altafjava.school.api.dto.response;

public record HostelBuildingResponse(
		String publicId,
		Long version,
		String name,
		String address,
		boolean active) {
}

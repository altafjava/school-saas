package com.altafjava.school.api.dto.response;

public record RoomResponse(
		String publicId,
		Long version,
		String hostelBuildingPublicId,
		String roomNumber,
		int capacity,
		boolean active) {
}

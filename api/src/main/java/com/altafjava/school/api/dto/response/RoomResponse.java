package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record RoomResponse(
		String publicId,
		Long version,
		String hostelBuildingPublicId,
		ReferenceSummary hostelBuilding,
		String roomNumber,
		int capacity,
		boolean active) {
}

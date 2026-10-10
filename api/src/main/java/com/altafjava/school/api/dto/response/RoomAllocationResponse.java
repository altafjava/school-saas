package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record RoomAllocationResponse(
		String publicId,
		String studentPublicId,
		String roomPublicId,
		LocalDate allocatedFrom,
		LocalDate allocatedUntil,
		boolean active) {
}

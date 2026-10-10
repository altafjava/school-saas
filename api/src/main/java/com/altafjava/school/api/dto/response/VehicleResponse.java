package com.altafjava.school.api.dto.response;

public record VehicleResponse(
		String publicId,
		Long version,
		String registrationNumber,
		int capacity,
		String driverName,
		String driverContact,
		boolean active) {
}

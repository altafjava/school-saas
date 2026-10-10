package com.altafjava.school.api.dto.response;

public record EmergencyContactResponse(
		String publicId,
		Long version,
		String name,
		String relationship,
		String phone,
		String alternatePhone,
		int priority) {
}

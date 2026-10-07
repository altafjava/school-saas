package com.altafjava.school.api.dto.response;

public record EmergencyContactResponse(
		String publicId,
		String name,
		String relationship,
		String phone,
		String alternatePhone,
		int priority) {
}

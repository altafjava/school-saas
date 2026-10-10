package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record VisitorLogResponse(
		String publicId,
		Long version,
		String visitorName,
		String visitorPhone,
		String purpose,
		String hostEmployeePublicId,
		String photoFilePublicId,
		boolean badgeIssued,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt) {
}

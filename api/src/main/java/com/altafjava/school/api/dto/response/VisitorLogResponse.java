package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record VisitorLogResponse(
		String publicId,
		String visitorName,
		String visitorPhone,
		String purpose,
		Long hostEmployeeId,
		String photoFilePublicId,
		boolean badgeIssued,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt) {
}

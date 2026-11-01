package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

public record VisitorLogResponse(
		String publicId,
		Long version,
		String visitorName,
		String visitorPhone,
		String purpose,
		String hostEmployeePublicId,
		ReferenceSummary hostEmployee,
		@NoLabel("Has no human name") String photoFilePublicId,
		boolean badgeIssued,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt) {
}

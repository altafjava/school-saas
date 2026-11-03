package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record ConversationResponse(
		String publicId,
		String guardianPublicId,
		ReferenceSummary guardian,
		String teacherPublicId,
		ReferenceSummary teacher,
		String studentPublicId,
		ReferenceSummary student,
		Instant lastMessageAt,
		long unreadCount) {
}

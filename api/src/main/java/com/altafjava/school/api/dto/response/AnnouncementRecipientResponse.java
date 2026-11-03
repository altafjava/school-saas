package com.altafjava.school.api.dto.response;

import java.time.Instant;
import com.altafjava.school.application.reference.ReferenceSummary;

public record AnnouncementRecipientResponse(
		String userPublicId,
		ReferenceSummary user,
		Instant notifiedAt,
		Instant readAt) {
}

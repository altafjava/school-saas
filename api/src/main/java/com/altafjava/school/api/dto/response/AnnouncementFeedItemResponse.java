package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.util.List;

public record AnnouncementFeedItemResponse(
		String publicId,
		String title,
		String body,
		boolean emergency,
		List<String> attachmentFilePublicIds,
		Instant publishedAt,
		Instant expiresAt,
		Instant readAt) {

	public AnnouncementFeedItemResponse {
		attachmentFilePublicIds = List.copyOf(attachmentFilePublicIds);
	}
}

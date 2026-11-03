package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.altafjava.school.application.reference.ReferenceSummary;

public record AnnouncementResponse(
		String publicId,
		Long version,
		String title,
		String body,
		String audienceType,
		Set<String> targets,
		boolean includeStudents,
		boolean emergency,
		Set<String> channels,
		List<String> attachmentFilePublicIds,
		String status,
		int recipientCount,
		Instant publishAt,
		Instant publishedAt,
		Instant expiresAt,
		String createdByUserPublicId,
		ReferenceSummary createdByUser) {

	public AnnouncementResponse {
		targets = Set.copyOf(targets);
		channels = Set.copyOf(channels);
		attachmentFilePublicIds = List.copyOf(attachmentFilePublicIds);
	}
}

package com.altafjava.school.api.dto.response;

import java.time.Instant;
import java.util.List;
import com.altafjava.school.application.reference.ReferenceSummary;

public record ConversationMessageResponse(
		String publicId,
		String senderUserPublicId,
		ReferenceSummary senderUser,
		String body,
		boolean erased,
		List<String> attachmentFilePublicIds,
		Instant sentAt) {

	public ConversationMessageResponse {
		attachmentFilePublicIds = List.copyOf(attachmentFilePublicIds);
	}
}

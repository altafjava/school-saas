package com.altafjava.school.api.dto.response;

import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;

public record TicketResponse(
		String publicId,
		Long version,
		String raisedByUserPublicId,
		TicketCategory category,
		String subject,
		String description,
		TicketStatus status,
		String assignedToUserPublicId,
		String resolution) {
}

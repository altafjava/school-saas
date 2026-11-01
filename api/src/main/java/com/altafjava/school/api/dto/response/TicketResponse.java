package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;

public record TicketResponse(
		String publicId,
		Long version,
		String raisedByUserPublicId,
		ReferenceSummary raisedByUser,
		TicketCategory category,
		String subject,
		String description,
		TicketStatus status,
		String assignedToUserPublicId,
		ReferenceSummary assignedToUser,
		String resolution) {
}

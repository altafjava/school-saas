package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record RoomAllocationResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String roomPublicId,
		ReferenceSummary room,
		LocalDate allocatedFrom,
		LocalDate allocatedUntil,
		boolean active) {
}

package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record BookCopyResponse(
		String publicId,
		Long version,
		String bookPublicId,
		ReferenceSummary book,
		String copyCode,
		String status) {
}

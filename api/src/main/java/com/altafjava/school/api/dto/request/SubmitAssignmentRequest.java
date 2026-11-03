package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An uploaded file, written text, or both; at least one is required")
public record SubmitAssignmentRequest(
		@Size(max = 500) String storageKey,
		@Size(max = 100_000) String textContent) {

	@Schema(hidden = true)
	@AssertTrue(message = "Send a file, text, or both")
	public boolean isNotEmpty() {
		return (storageKey != null && !storageKey.isBlank()) || (textContent != null && !textContent.isBlank());
	}
}

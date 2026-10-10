package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateCounselingSessionNotesRequest(
		@Size(max = 2000) String notes,
		boolean followUpRequired,
		@NotNull Long version) implements Versioned {
}

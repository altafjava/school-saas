package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpsertHealthRecordRequest(
		@Size(max = 10) String bloodGroup,
		@Size(max = 1000) String allergies,
		@Size(max = 1000) String conditions,
		@Size(max = 1000) String immunizations,
		// Null on the first save, when no record exists yet.
		Long version) implements Versioned {
}

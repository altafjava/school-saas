package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateAlumniContactInfoRequest(
		@Size(max = 255) String currentOccupation,
		@Email @Size(max = 255) String contactEmail,
		@Size(max = 50) String contactPhone,
		@NotNull Long version) implements Versioned {
}

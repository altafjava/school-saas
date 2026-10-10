package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateEventRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 1000) String description,
		@NotNull LocalDateTime eventDate,
		@Size(max = 200) String location,
		@NotNull Long version) implements Versioned {
}

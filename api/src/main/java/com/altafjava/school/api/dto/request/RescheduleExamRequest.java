package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record RescheduleExamRequest(@NotNull LocalDateTime scheduledAt,
		@NotNull Long version) implements Versioned {
}

package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record RescheduleAssignmentRequest(@NotNull LocalDate dueDate,
		@NotNull Long version) implements Versioned {
}

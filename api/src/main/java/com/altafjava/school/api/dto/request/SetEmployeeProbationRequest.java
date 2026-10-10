package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record SetEmployeeProbationRequest(@NotNull LocalDate probationEndDate,
		@NotNull Long version) implements Versioned {
}

package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateClassroomCapacityRequest(@Min(1) Integer capacity,
		@NotNull Long version) implements Versioned {
}

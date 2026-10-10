package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record AssignExamTermRequest(@NotBlank String termPublicId,
		@NotNull Long version) implements Versioned {
}

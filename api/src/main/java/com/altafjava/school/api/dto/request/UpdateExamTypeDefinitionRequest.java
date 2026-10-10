package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateExamTypeDefinitionRequest(
		@NotBlank String name,
		boolean active,
		int displayOrder,
		@NotNull Long version) implements Versioned {
}

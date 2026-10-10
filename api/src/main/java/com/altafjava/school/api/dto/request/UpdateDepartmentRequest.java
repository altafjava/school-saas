package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateDepartmentRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 50) String code,
		@Size(max = 500) String description,
		@NotNull Long version) implements Versioned {
}

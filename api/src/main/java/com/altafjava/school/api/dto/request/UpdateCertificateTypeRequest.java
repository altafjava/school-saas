package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateCertificateTypeRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank String wording,
		@NotNull Long version) implements Versioned {
}

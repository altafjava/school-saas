package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCertificateTypeRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank String wording) {
}

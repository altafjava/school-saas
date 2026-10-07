package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCertificateTypeRequest(
		@NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "must be 2-50 characters of A-Z, 0-9 and _, starting with a letter") String code,
		@NotBlank @Size(max = 150) String name,
		@NotBlank String wording) {
}

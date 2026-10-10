package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;
import com.altafjava.school.domain.common.model.Gender;

public record UpdateEmployeeContactDetailsRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 255) String email,
		@NotNull Gender gender,
		@NotNull Long version) implements Versioned {
}

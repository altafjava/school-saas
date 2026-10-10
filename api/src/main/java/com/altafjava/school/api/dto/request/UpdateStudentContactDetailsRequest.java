package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateStudentContactDetailsRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Email @Size(max = 255) String email,
		@NotNull LocalDate dateOfBirth,
		@NotNull Long version) implements Versioned {
}

package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateHolidayRequest(
		@NotNull LocalDate date,
		@NotBlank @Size(max = 200) String name,
		boolean recurring,
		@NotNull Long version) implements Versioned {
}

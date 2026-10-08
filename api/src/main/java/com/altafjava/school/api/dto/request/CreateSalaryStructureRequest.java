package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateSalaryStructureRequest(
		@NotBlank String employeePublicId,
		@NotEmpty @Valid List<PayComponentAmountRequest> components,
		@NotNull LocalDate effectiveFrom) {

	public CreateSalaryStructureRequest {
		components = components == null ? null : List.copyOf(components);
	}
}

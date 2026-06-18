package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeeStructureRequest(
		@NotBlank @Size(max = 200) String name,
		@NotNull @DecimalMin("0.01") BigDecimal amount,
		@NotBlank String frequency,
		@Size(max = 100) String planType) {
}

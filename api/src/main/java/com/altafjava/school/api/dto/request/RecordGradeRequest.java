package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecordGradeRequest(
		@NotBlank String studentPublicId,
		@NotBlank String examPublicId,
		@NotNull @DecimalMin("0.0") BigDecimal marks) {
}

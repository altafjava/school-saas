package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record CorrectGradeRequest(
		@NotNull @DecimalMin("0.0") BigDecimal marks,
		@NotNull Long version) implements Versioned {
}

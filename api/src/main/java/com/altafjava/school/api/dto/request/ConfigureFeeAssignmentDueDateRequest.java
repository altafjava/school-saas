package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record ConfigureFeeAssignmentDueDateRequest(
		@NotNull LocalDate dueDate,
		@Min(0) Integer graceDays,
		@DecimalMin("0.0") @DecimalMax("100.0") BigDecimal lateFeePercentage,
		@NotNull Long version) implements Versioned {
}

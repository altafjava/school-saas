package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateLeaveTypeRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal defaultAnnualDays,
		@NotNull Long version) implements Versioned {
}

package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record ConfigureLeaveCarryForwardRequest(
		boolean enabled,
		@DecimalMin("0.0") BigDecimal maxCarryForwardDays,
		@Min(1) Integer carryForwardExpiryMonths,
		@NotNull Long version) implements Versioned {
}

package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record ReviseFeeAmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,
		@NotNull Long version) implements Versioned {
}

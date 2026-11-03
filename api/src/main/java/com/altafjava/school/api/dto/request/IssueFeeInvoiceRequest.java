package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueFeeInvoiceRequest(@NotBlank String studentPublicId, @NotNull LocalDate periodStart,
		@NotNull LocalDate periodEnd, @NotNull LocalDate dueDate) {
}

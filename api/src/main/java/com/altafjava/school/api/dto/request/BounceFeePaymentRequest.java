package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BounceFeePaymentRequest(
		@NotBlank @Size(max = 500) String reason,
		@NotNull LocalDateTime bouncedAt) {
}

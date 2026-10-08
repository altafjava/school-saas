package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundFeePaymentRequest(
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@NotBlank @Size(max = 500) String reason) {
}

package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordFeePaymentRequest(
		@NotNull Long studentId,
		@NotNull Long feeStructureId,
		@NotNull @DecimalMin("0.01") BigDecimal paidAmount,
		@NotNull LocalDateTime paidAt,
		@NotBlank @Size(max = 100) String receiptNumber) {
}

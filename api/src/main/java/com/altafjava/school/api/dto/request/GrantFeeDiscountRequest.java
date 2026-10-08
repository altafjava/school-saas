package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.fee.model.DiscountType;

public record GrantFeeDiscountRequest(
		@NotBlank @Size(max = 36) String feeStructurePublicId,
		@NotNull DiscountType type,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal value,
		@NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,49}$", message = "must be 2-50 letters, digits or _") String category,
		@NotBlank @Size(max = 500) String reason) {
}

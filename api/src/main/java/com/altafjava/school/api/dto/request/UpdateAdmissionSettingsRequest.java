package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateAdmissionSettingsRequest(
		@Schema(description = "Fee charged per application; null or 0 charges no fee. Applies to applications submitted from now on.") @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal applicationFeeAmount) {
}

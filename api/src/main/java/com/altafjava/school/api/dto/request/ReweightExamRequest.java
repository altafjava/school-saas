package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ReweightExamRequest(@NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal weightage) {
}

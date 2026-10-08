package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeFeeDiscountRequest(@NotBlank @Size(max = 500) String reason) {
}

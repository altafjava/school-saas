package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReserveBookRequest(@NotBlank String bookPublicId, @NotBlank String studentPublicId) {
}

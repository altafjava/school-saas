package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The reason is mandatory: a custody restriction without a recorded basis is not defensible later.
public record RestrictCustodyRequest(@NotBlank @Size(max = 500) String note) {
}

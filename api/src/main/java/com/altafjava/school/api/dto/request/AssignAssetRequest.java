package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.inventory.model.AssignedToType;

public record AssignAssetRequest(
		@NotNull AssignedToType assignedToType,
		@NotBlank String assignedToPublicId,
		@NotNull LocalDate assignedAt) {
}

package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateVehicleRequest(
		@Min(1) int capacity,
		@Size(max = 100) String driverName,
		@Size(max = 50) String driverContact,
		@NotNull Long version) implements Versioned {
}

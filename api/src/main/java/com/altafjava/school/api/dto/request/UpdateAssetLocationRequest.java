package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateAssetLocationRequest(@Size(max = 150) String location,
		@NotNull Long version) implements Versioned {
}

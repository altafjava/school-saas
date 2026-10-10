package com.altafjava.school.api.dto.request;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdateGradingScaleThresholdsRequest(
		@NotEmpty @Valid List<GradingScaleThresholdRequest> thresholds,
		@NotNull Long version) implements Versioned {

	public UpdateGradingScaleThresholdsRequest {
		thresholds = thresholds == null ? null : List.copyOf(thresholds);
	}
}

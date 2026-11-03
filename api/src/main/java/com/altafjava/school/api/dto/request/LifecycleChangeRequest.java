package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record LifecycleChangeRequest(
		@Size(max = 500) String reason,
		@Schema(description = "Today or earlier at the school; defaults to today") LocalDate effectiveOn) {

	public static final LifecycleChangeRequest EMPTY = new LifecycleChangeRequest(null, null);
}

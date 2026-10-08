package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record LifecycleChangeRequest(
		@Size(max = 500) String reason,
		@PastOrPresent LocalDate effectiveOn) {

	public static final LifecycleChangeRequest EMPTY = new LifecycleChangeRequest(null, null);
}

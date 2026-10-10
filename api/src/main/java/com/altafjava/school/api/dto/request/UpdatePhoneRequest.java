package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

public record UpdatePhoneRequest(@Size(max = 30) String phone,
		@NotNull Long version) implements Versioned {
}

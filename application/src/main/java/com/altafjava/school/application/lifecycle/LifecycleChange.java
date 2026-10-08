package com.altafjava.school.application.lifecycle;

import java.time.LocalDate;
import com.altafjava.platform.core.exception.BusinessException;

/** Why and from when a lifecycle change applies. Both are optional; the date defaults to today. */
public record LifecycleChange(String reason, LocalDate effectiveOn) {

	public static final LifecycleChange NONE = new LifecycleChange(null, null);

	public LifecycleChange {
		if (effectiveOn != null && effectiveOn.isAfter(LocalDate.now())) {
			throw new BusinessException("A lifecycle change cannot take effect in the future: " + effectiveOn);
		}
	}

	public static LifecycleChange of(String reason) {
		return new LifecycleChange(reason, null);
	}
}

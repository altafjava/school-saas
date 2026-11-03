package com.altafjava.school.application.lifecycle;

import java.time.LocalDate;

/**
 * Why and from when a lifecycle change applies. Both are optional; the date defaults to the school's
 * today, and {@link LifecycleRecorder} refuses a future one.
 */
public record LifecycleChange(String reason, LocalDate effectiveOn) {

	public static final LifecycleChange NONE = new LifecycleChange(null, null);

	public static LifecycleChange of(String reason) {
		return new LifecycleChange(reason, null);
	}
}

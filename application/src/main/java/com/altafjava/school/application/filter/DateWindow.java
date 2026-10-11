package com.altafjava.school.application.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An optional, inclusive date range for a list filter; either end may be open. Timestamp columns hold the
 * server's local time, so the day bounds are taken in the same zone.
 */
public record DateWindow(LocalDate from, LocalDate to) {

	public static final DateWindow UNBOUNDED = new DateWindow(null, null);

	public DateWindow {
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
	}

	/** First instant of {@code from}, or null when the range is open at the start. */
	public LocalDateTime startInclusive() {
		return from != null ? from.atStartOfDay() : null;
	}

	/** First instant after {@code to}, or null when the range is open at the end. */
	public LocalDateTime endExclusive() {
		return to != null ? to.plusDays(1).atStartOfDay() : null;
	}
}

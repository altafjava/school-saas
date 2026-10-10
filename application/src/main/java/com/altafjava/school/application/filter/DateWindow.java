package com.altafjava.school.application.filter;

import java.time.LocalDate;

/** An optional, inclusive date range for a list filter; either end may be open. */
public record DateWindow(LocalDate from, LocalDate to) {

	public static final DateWindow UNBOUNDED = new DateWindow(null, null);

	public DateWindow {
		if (from != null && to != null && from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
	}
}

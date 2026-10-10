package com.altafjava.school.application.filter;

/** Narrows the event list; every part is optional. */
public record EventFilter(DateWindow dates, String q) {

	public static final EventFilter NONE = new EventFilter(DateWindow.UNBOUNDED, null);
}

package com.altafjava.school.application.filter;

/** Narrows the holiday list; every part is optional. */
public record HolidayFilter(DateWindow dates, String q) {

	public static final HolidayFilter NONE = new HolidayFilter(DateWindow.UNBOUNDED, null);
}

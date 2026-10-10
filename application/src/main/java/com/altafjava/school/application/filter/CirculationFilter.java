package com.altafjava.school.application.filter;

/**
 * Narrows the circulation (loan) list; every part is optional. {@code returned} separates loans handed back
 * ({@code true}) from loans still out ({@code false}); the dates bound the check-out day.
 */
public record CirculationFilter(String studentPublicId, String bookPublicId, Boolean returned, DateWindow dates) {

	public static final CirculationFilter NONE = new CirculationFilter(null, null, null, DateWindow.UNBOUNDED);
}

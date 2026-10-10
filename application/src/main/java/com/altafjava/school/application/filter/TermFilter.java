package com.altafjava.school.application.filter;

/** Narrows the term list; every part is optional. */
public record TermFilter(String academicYearPublicId, String q) {

	public static final TermFilter NONE = new TermFilter(null, null);
}

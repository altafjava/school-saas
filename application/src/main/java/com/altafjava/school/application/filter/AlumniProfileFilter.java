package com.altafjava.school.application.filter;

/** Narrows the alumni-profile list; every part is optional. */
public record AlumniProfileFilter(Integer graduationYear, Boolean active, String q) {

	public static final AlumniProfileFilter NONE = new AlumniProfileFilter(null, null, null);
}

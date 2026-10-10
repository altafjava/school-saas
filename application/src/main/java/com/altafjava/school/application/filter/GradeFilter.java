package com.altafjava.school.application.filter;

/** Narrows the grade list; every part is optional and composes with the caller's exam and student scope. */
public record GradeFilter(String examPublicId, String studentPublicId, String classroomPublicId) {

	public static final GradeFilter NONE = new GradeFilter(null, null, null);
}

package com.altafjava.school.application.filter;

/**
 * Narrows the assignment and lesson lists; every part is optional. Without a classroom the list covers every
 * classroom the caller can reach; with one, the caller must be able to reach it.
 */
public record CourseworkFilter(String classroomPublicId, String subjectPublicId, DateWindow dates) {

	public static final CourseworkFilter NONE = new CourseworkFilter(null, null, DateWindow.UNBOUNDED);
}

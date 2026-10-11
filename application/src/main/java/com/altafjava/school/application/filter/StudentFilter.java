package com.altafjava.school.application.filter;

import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.student.model.EnrollmentStatus;

/** Narrows the student list; every part is optional and {@code q} (blank = no filter) matches code, name and email. */
public record StudentFilter(EnrollmentStatus status, String classroomPublicId, Gender gender, String q) {

	public static final StudentFilter NONE = new StudentFilter(null, null, null, null);

	public static StudentFilter matching(String q) {
		return new StudentFilter(null, null, null, q);
	}
}

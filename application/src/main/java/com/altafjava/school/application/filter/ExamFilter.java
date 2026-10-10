package com.altafjava.school.application.filter;

import com.altafjava.school.domain.exam.model.ExamStatus;

/** Narrows the exam list; every part is optional. */
public record ExamFilter(String classroomPublicId, String termPublicId, String subjectPublicId, ExamStatus status) {

	public static final ExamFilter NONE = new ExamFilter(null, null, null, null);
}

package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record ClassroomResponse(
		String publicId,
		Long version,
		String classCode,
		String grade,
		String section,
		String academicYear,
		String classTeacherPublicId,
		ReferenceSummary classTeacher,
		String curriculumPublicId,
		ReferenceSummary curriculum,
		Integer capacity) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record StudentClassroomLinkResponse(
		String publicId,
		String studentPublicId,
		String classroomPublicId,
		String academicYearPublicId,
		LocalDate enrolledAt,
		String rollNumber) {
}

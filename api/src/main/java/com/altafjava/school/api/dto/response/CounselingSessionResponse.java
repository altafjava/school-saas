package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record CounselingSessionResponse(
		String publicId,
		String studentPublicId,
		String counselorTeacherPublicId,
		LocalDate sessionDate,
		String notes,
		boolean followUpRequired) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record CounselingSessionResponse(
		String publicId,
		Long version,
		String studentPublicId,
		String counselorTeacherPublicId,
		LocalDate sessionDate,
		String notes,
		boolean followUpRequired) {
}

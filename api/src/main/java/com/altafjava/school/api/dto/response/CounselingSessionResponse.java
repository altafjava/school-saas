package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record CounselingSessionResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String counselorTeacherPublicId,
		ReferenceSummary counselorTeacher,
		LocalDate sessionDate,
		String notes,
		boolean followUpRequired) {
}

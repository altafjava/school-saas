package com.altafjava.school.api.dto.request;

import java.time.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTimetableEntryRequest(
		@NotNull DayOfWeek dayOfWeek,
		@NotBlank String periodPublicId,
		@NotBlank String classroomPublicId,
		@NotBlank String subjectPublicId,
		@NotBlank String teacherPublicId,
		String venuePublicId) {
}

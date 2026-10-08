package com.altafjava.school.api.dto.request;

import java.time.DayOfWeek;
import jakarta.validation.constraints.NotNull;

public record CreateTimetableEntryRequest(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull Long periodId,
		@NotNull Long classroomId,
		@NotNull Long subjectId,
		@NotNull Long teacherId,
		String venuePublicId) {
}

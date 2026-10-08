package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignSubstituteRequest(
		@NotBlank String timetableEntryPublicId,
		@NotNull LocalDate date,
		@NotBlank String substituteTeacherPublicId,
		@Size(max = 500) String reason) {
}

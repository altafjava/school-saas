package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record MoveClassroomAcademicYearRequest(@NotBlank String academicYearPublicId,
		@NotNull Long version) implements Versioned {
}

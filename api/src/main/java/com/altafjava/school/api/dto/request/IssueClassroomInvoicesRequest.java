package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IssueClassroomInvoicesRequest(@NotBlank String classroomPublicId, @NotNull LocalDate periodStart,
		@NotNull LocalDate periodEnd, @NotNull LocalDate dueDate) {
}

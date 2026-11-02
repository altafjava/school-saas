package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitStudentLeaveRequest(
		@NotBlank String studentPublicId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@NotBlank @Size(max = 500) String reason,
		String attachmentFilePublicId) {
}

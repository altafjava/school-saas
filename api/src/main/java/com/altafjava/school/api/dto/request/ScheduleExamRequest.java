package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleExamRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 100) String subject,
		@NotNull Long classroomId,
		@NotNull LocalDateTime scheduledAt,
		@NotNull @DecimalMin("1.0") BigDecimal maxMarks) {
}

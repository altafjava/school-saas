package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleExamRequest(
		@NotBlank @Size(max = 200) String title,
		@NotNull Long subjectId,
		@NotNull Long classroomId,
		@NotNull LocalDateTime scheduledAt,
		@NotNull @DecimalMin("1.0") BigDecimal maxMarks,
		Long termId,
		@NotNull Long examTypeId,
		@DecimalMin("0.01") @DecimalMax("100") BigDecimal weightage) {
}

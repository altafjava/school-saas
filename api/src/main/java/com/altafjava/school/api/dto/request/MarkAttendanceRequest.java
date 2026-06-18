package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarkAttendanceRequest(
		@NotNull Long studentId,
		@NotNull Long classroomId,
		@NotNull LocalDate attendanceDate,
		@NotBlank String status,
		String markedBy) {
}

package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public record BulkMarkAttendanceRequest(
		@NotBlank String classroomPublicId,
		@NotNull LocalDate attendanceDate,
		@NotEmpty @Size(max = 500) List<@Valid Row> rows) {

	public BulkMarkAttendanceRequest {
		rows = rows == null ? null : List.copyOf(rows);
	}

	public record Row(@NotBlank String studentPublicId, @NotNull AttendanceStatus status) {
	}
}

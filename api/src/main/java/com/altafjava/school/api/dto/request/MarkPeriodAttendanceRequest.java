package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

public record MarkPeriodAttendanceRequest(
		@NotBlank String studentPublicId,
		@NotBlank String timetableEntryPublicId,
		@NotNull LocalDate attendanceDate,
		@NotNull AttendanceStatus status) {
}

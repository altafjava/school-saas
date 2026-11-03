package com.altafjava.school.api.dto.request;

import java.time.LocalTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffWorkdayRequest(@NotNull LocalTime start, @NotNull LocalTime end,
		@NotNull @Min(0) @Max(240) Integer lateGraceMinutes, @NotNull @Min(1) @Max(720) Integer halfDayMinutes) {
}

package com.altafjava.school.api.dto.response.staffattendance;

import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The school's staff workday: arriving after start plus the grace is late, leaving before end "
		+ "is early, and working fewer than the half-day minutes makes a half day.")
public record StaffWorkdayResponse(LocalTime start, LocalTime end, int lateGraceMinutes, int halfDayMinutes) {
}

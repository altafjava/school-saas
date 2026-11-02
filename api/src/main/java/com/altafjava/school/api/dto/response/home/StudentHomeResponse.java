package com.altafjava.school.api.dto.response.home;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The student home screen.")
public record StudentHomeResponse(@Schema(description = "Today at the school") LocalDate date,
		@Schema(description = "False on Sundays and holidays") boolean schoolDay,
		@Schema(description = "Absent when the caller's role may not read it") List<TimetableSlotResponse> timetable,
		StudentOverviewResponse overview, InboxResponse inbox) {

	public StudentHomeResponse {
		timetable = timetable == null ? null : List.copyOf(timetable);
	}
}

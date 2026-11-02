package com.altafjava.school.api.dto.response.home;

import java.time.LocalTime;
import com.altafjava.school.application.home.SlotCoverage;
import com.altafjava.school.application.reference.ReferenceSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One period of today's timetable, after substitutions.")
public record TimetableSlotResponse(String periodName, LocalTime startTime, LocalTime endTime,
		ReferenceSummary classroom, ReferenceSummary subject,
		@Schema(description = "Who teaches the slot today") ReferenceSummary teacher, String venueName,
		SlotCoverage coverage) {
}

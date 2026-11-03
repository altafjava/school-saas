package com.altafjava.school.api.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Replaces the exam's slot. Leaving out the venue takes the exam out of its room.")
public record RescheduleExamRequest(@NotNull LocalDateTime scheduledAt,
		@NotNull @Min(5) @Max(720) Integer durationMinutes,
		String venuePublicId,
		@NotNull Long version) implements Versioned {
}

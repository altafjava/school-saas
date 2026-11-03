package com.altafjava.school.api.dto.request;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;

public record ScheduleAnnouncementRequest(@NotNull Instant publishAt) {
}

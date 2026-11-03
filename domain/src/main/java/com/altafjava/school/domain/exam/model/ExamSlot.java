package com.altafjava.school.domain.exam.model;

import java.time.LocalDateTime;

/** When and where an exam sits. {@code venueId} is optional. */
public record ExamSlot(LocalDateTime startsAt, int durationMinutes, Long venueId) {
}

package com.altafjava.school.domain.lms.model;

import java.time.LocalDate;
import java.util.UUID;

/** One assignment with work still to grade. */
public record GradingQueueEntry(UUID assignmentPublicId, String title, Long classroomId, Long subjectId,
		LocalDate dueDate, long ungradedCount) {
}

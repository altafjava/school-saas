package com.altafjava.school.domain.exam.event;

import java.time.Instant;
import java.util.List;

// Published after a classroom's exam dates become visible to its students and guardians.
public record ExamSchedulePublishedEvent(Long tenantId, Long classroomId, List<Long> examIds, Instant timestamp) {

	public ExamSchedulePublishedEvent {
		examIds = List.copyOf(examIds);
	}

	public ExamSchedulePublishedEvent(Long tenantId, Long classroomId, List<Long> examIds) {
		this(tenantId, classroomId, examIds, Instant.now());
	}
}

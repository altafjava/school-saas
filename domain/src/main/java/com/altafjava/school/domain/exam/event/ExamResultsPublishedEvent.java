package com.altafjava.school.domain.exam.event;

import java.time.Instant;

// Published after an exam's results become visible to students and guardians.
public record ExamResultsPublishedEvent(
		Long tenantId,
		Long examId,
		Instant timestamp) {

	public ExamResultsPublishedEvent(Long tenantId, Long examId) {
		this(tenantId, examId, Instant.now());
	}
}

package com.altafjava.school.domain.classroom.event;

import java.time.Instant;

/** A student moved up into a classroom of a later academic year. */
public record StudentPromotedEvent(
		Long tenantId,
		Long studentId,
		Long fromClassroomId,
		Long toClassroomId,
		Long academicYearId,
		Instant timestamp) {

	public StudentPromotedEvent(Long tenantId, Long studentId, Long fromClassroomId, Long toClassroomId,
			Long academicYearId) {
		this(tenantId, studentId, fromClassroomId, toClassroomId, academicYearId, Instant.now());
	}
}

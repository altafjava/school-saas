package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.application.reference.ReferenceSummary;

public record LessonResponse(
		String publicId,
		Long version,
		String classroomPublicId,
		ReferenceSummary classroom,
		String subjectPublicId,
		ReferenceSummary subject,
		String teacherPublicId,
		ReferenceSummary teacher,
		String title,
		String description,
		String storageKey,
		LocalDateTime postedAt) {
}

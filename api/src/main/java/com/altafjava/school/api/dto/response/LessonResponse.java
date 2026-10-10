package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;

public record LessonResponse(
		String publicId,
		String classroomPublicId,
		String subjectPublicId,
		String teacherPublicId,
		String title,
		String description,
		String storageKey,
		LocalDateTime postedAt) {
}

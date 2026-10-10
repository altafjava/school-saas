package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssignmentResponse(
		String publicId,
		String classroomPublicId,
		String subjectPublicId,
		String teacherPublicId,
		String title,
		String description,
		String storageKey,
		LocalDate dueDate,
		BigDecimal maxMarks) {
}

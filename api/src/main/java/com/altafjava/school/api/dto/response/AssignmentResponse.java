package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.altafjava.school.application.reference.ReferenceSummary;

public record AssignmentResponse(
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
		LocalDate dueDate,
		BigDecimal maxMarks) {
}

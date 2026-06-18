package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassroomRequest(
		@NotBlank @Size(max = 50) String classCode,
		@NotBlank @Size(max = 20) String grade,
		@NotBlank @Size(max = 10) String section,
		@NotBlank @Size(max = 20) String academicYear,
		Long classTeacherId) {
}

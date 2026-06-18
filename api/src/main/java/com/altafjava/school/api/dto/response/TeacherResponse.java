package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TeacherResponse(
		String publicId,
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		LocalDate joinDate) {
}

package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record TeacherResponse(
		String publicId,
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		String phone,
		LocalDate joinDate,
		Long departmentId,
		String qualification,
		String employmentType,
		AddressResponse address,
		LocalDate probationEndDate,
		String photoFilePublicId) {
}

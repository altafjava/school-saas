package com.altafjava.school.api.dto.response;

import java.time.LocalDate;

public record EmployeeResponse(
		String publicId,
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		String phone,
		LocalDate joinDate,
		Long departmentId,
		String designation,
		String qualification,
		String employmentType,
		String staffCategory,
		String status,
		AddressResponse address,
		LocalDate probationEndDate,
		LocalDate exitDate,
		String exitReason,
		String photoFilePublicId) {
}

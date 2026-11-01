package com.altafjava.school.api.dto.response;

import java.time.LocalDate;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.common.model.Gender;

public record TeacherResponse(
		String publicId,
		Long version,
		String employeeCode,
		String firstName,
		String lastName,
		String email,
		Gender gender,
		String phone,
		LocalDate joinDate,
		String departmentPublicId,
		ReferenceSummary department,
		String designation,
		String qualification,
		String employmentType,
		AddressResponse address,
		String status,
		LocalDate probationEndDate,
		@NoLabel("Has no human name") String photoFilePublicId) {
}

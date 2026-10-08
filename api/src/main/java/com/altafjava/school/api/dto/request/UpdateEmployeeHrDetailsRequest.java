package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.employee.model.EmploymentType;

public record UpdateEmployeeHrDetailsRequest(
		String departmentPublicId,
		@Size(max = 100) String designation,
		@Size(max = 255) String qualification,
		EmploymentType employmentType) {
}

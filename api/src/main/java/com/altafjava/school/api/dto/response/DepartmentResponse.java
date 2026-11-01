package com.altafjava.school.api.dto.response;

import com.altafjava.school.application.reference.ReferenceSummary;

public record DepartmentResponse(
		String publicId,
		Long version,
		String name,
		String code,
		String description,
		String headEmployeePublicId,
		ReferenceSummary headEmployee,
		boolean active) {
}

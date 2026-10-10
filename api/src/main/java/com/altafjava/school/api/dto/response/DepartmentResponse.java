package com.altafjava.school.api.dto.response;

public record DepartmentResponse(
		String publicId,
		String name,
		String code,
		String description,
		String headEmployeePublicId,
		boolean active) {
}

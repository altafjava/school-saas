package com.altafjava.school.api.dto.response;

public record AvailableTeacherResponse(String publicId,
		Long version, String employeeCode, String name) {
}

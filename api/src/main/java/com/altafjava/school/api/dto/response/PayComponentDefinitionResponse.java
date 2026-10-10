package com.altafjava.school.api.dto.response;

import com.altafjava.school.domain.payroll.model.PayComponentType;

public record PayComponentDefinitionResponse(
		String publicId,
		Long version,
		String code,
		String name,
		PayComponentType type,
		int displayOrder,
		boolean active) {
}

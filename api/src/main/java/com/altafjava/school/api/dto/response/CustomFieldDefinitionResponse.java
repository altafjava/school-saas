package com.altafjava.school.api.dto.response;

public record CustomFieldDefinitionResponse(
		String publicId,
		Long version,
		String entityType,
		String fieldKey,
		String label,
		String fieldType,
		boolean required,
		boolean active,
		CustomFieldValidationRuleResponse validationRule,
		int displayOrder,
		String displayGroup,
		int displayGroupOrder,
		CustomFieldVisibilityConditionResponse visibilityCondition) {
}

package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.altafjava.platform.application.extension.EntityAttributeService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.customfield.CustomFieldValue;
import com.altafjava.school.application.customfield.FieldGroup;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.customfield.model.CustomFieldValidationRule;
import com.altafjava.school.domain.customfield.repository.CustomFieldDefinitionRepository;
import com.altafjava.school.domain.error.SchoolErrorCode;

/**
 * Wraps platform's generic {@link EntityAttributeService} (Pattern B of the entity-extension
 * contract — a sparse tenant-scoped key/value store, {@code entity_extended_attributes}) under a
 * fixed {@code domain} namespace, so school-saas's custom fields never collide with any other
 * domain project's use of the same generic mechanism.
 *
 * <p>
 * Every read/write here is validated against an active {@link CustomFieldDefinition} first — this
 * service is the only allowed writer of school custom-field values specifically so that
 * validation can never be bypassed by calling {@link EntityAttributeService} directly with this
 * namespace. Unknown/inactive field keys and type-mismatched values fail fast with a
 * {@link BusinessException} (400) rather than being silently stored as opaque strings.
 */
@Service
public class CustomFieldValueService {

	// Namespaces every school-saas custom-field value in platform's shared extension table,
	// isolating it from any other domain/feature that might also use EntityAttributeService.
	static final String DOMAIN = "school-custom-fields";

	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
	private final EntityAttributeService entityAttributeService;

	public CustomFieldValueService(CustomFieldDefinitionRepository customFieldDefinitionRepository,
			EntityAttributeService entityAttributeService) {
		this.customFieldDefinitionRepository = customFieldDefinitionRepository;
		this.entityAttributeService = entityAttributeService;
	}

	public void setValue(CustomFieldEntityType entityType, Long entityId, String fieldKey, String rawValue) {
		Long tenantId = TenantContext.getCurrentTenantId();
		CustomFieldDefinition definition = requireActiveDefinition(tenantId, entityType, fieldKey);
		if (definition.isRequired() && (rawValue == null || rawValue.isBlank())) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_REQUIRED,
					"Custom field '" + fieldKey + "' is required and cannot be cleared", Map.of("field", fieldKey));
		}
		if (rawValue != null && !rawValue.isBlank()) {
			validateValue(definition, rawValue);
		}
		entityAttributeService.setAttribute(tenantId, entityId, entityType.name(), DOMAIN, fieldKey, rawValue);
	}

	public Optional<String> getValue(CustomFieldEntityType entityType, Long entityId, String fieldKey) {
		Long tenantId = TenantContext.getCurrentTenantId();
		requireActiveDefinition(tenantId, entityType, fieldKey);
		return entityAttributeService.getAttribute(tenantId, entityId, entityType.name(), DOMAIN, fieldKey);
	}

	/**
	 * Merges every active definition for {@code entityType} with whatever values are actually
	 * stored for {@code entityId} — a field with no stored value still appears, with a {@code null}
	 * value, so a caller can distinguish "defined but unset" from "not a defined field at all".
	 */
	public List<CustomFieldValue> getAllValues(CustomFieldEntityType entityType, Long entityId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		List<CustomFieldDefinition> definitions = customFieldDefinitionRepository
				.findAllByTenantIdAndEntityTypeAndActiveTrueOrderByDisplayOrderAsc(tenantId, entityType);
		Map<String, String> storedValues = entityAttributeService.getAttributes(tenantId, entityId, entityType.name(),
				DOMAIN);
		return definitions.stream().map(definition -> toCustomFieldValue(definition, storedValues)).toList();
	}

	/**
	 * Same merge as {@link #getAllValues}, additionally grouped into ordered {@link FieldGroup}s
	 * (by {@code displayGroup}, ordered by the lowest {@code displayGroupOrder} among its fields —
	 * fields with no group are collected under {@code ""}) so a caller renders form sections
	 * exactly as configured instead of re-deriving grouping/ordering itself.
	 */
	public List<FieldGroup> getGroupedValues(CustomFieldEntityType entityType, Long entityId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		List<CustomFieldDefinition> definitions = customFieldDefinitionRepository
				.findAllByTenantIdAndEntityTypeAndActiveTrueOrderByDisplayOrderAsc(tenantId, entityType);
		Map<String, String> storedValues = entityAttributeService.getAttributes(tenantId, entityId, entityType.name(),
				DOMAIN);

		Map<String, List<CustomFieldDefinition>> byGroup = new LinkedHashMap<>();
		for (CustomFieldDefinition definition : definitions) {
			String groupName = definition.getDisplayGroup() != null ? definition.getDisplayGroup() : "";
			byGroup.computeIfAbsent(groupName, key -> new java.util.ArrayList<>()).add(definition);
		}

		return byGroup.entrySet().stream()
				.map(entry -> new FieldGroup(entry.getKey(), groupOrder(entry.getValue()),
						entry.getValue().stream().map(definition -> toCustomFieldValue(definition, storedValues))
								.toList()))
				.sorted(Comparator.comparingInt(FieldGroup::order))
				.toList();
	}

	private int groupOrder(List<CustomFieldDefinition> definitionsInGroup) {
		return definitionsInGroup.stream().mapToInt(CustomFieldDefinition::getDisplayGroupOrder).min().orElse(0);
	}

	private CustomFieldValue toCustomFieldValue(CustomFieldDefinition definition, Map<String, String> storedValues) {
		String value = storedValues.get(definition.getFieldKey());
		boolean visible = definition.getVisibilityCondition().isPresent()
				? definition.getVisibilityCondition()
						.isSatisfiedBy(storedValues.get(definition.getVisibilityCondition().getDependsOnFieldKey()))
				: true;
		return new CustomFieldValue(definition.getFieldKey(), definition.getLabel(), definition.getFieldType(),
				definition.isRequired(), value, definition.getValidationRule().optionList(),
				definition.getDisplayOrder(), definition.getDisplayGroup(), visible);
	}

	private CustomFieldDefinition requireActiveDefinition(Long tenantId, CustomFieldEntityType entityType,
			String fieldKey) {
		CustomFieldDefinition definition = customFieldDefinitionRepository
				.findByTenantIdAndEntityTypeAndFieldKey(tenantId, entityType, fieldKey)
				.orElseThrow(() -> new BusinessException(SchoolErrorCode.CUSTOM_FIELD_UNKNOWN,
						"Unknown custom field for " + entityType + ": " + fieldKey, Map.of("field", fieldKey)));
		if (!definition.isActive()) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_UNKNOWN, "Custom field is not active: " + fieldKey,
					Map.of("field", fieldKey));
		}
		return definition;
	}

	private void validateValue(CustomFieldDefinition definition, String rawValue) {
		CustomFieldType fieldType = definition.getFieldType();
		CustomFieldValidationRule rule = definition.getValidationRule();
		try {
			switch (fieldType) {
				case NUMBER -> validateNumber(definition, rule, rawValue);
				case DATE -> LocalDate.parse(rawValue);
				case BOOLEAN -> {
					if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
						throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_TYPE_MISMATCH,
								"Custom field '" + definition.getFieldKey() + "' expects a BOOLEAN (true/false)"
										+ " value, got: " + rawValue,
								Map.of("field", definition.getFieldKey(), "type", "BOOLEAN"));
					}
				}
				case TEXT -> validateText(definition, rule, rawValue);
				case SELECT -> validateOption(definition, rule, rawValue);
				case MULTI_SELECT -> Arrays.stream(rawValue.split(","))
						.map(String::trim)
						.filter(value -> !value.isEmpty())
						.forEach(value -> validateOption(definition, rule, value));
			}
		} catch (NumberFormatException e) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_TYPE_MISMATCH,
					"Custom field '" + definition.getFieldKey() + "' expects a NUMBER value, got: " + rawValue,
					Map.of("field", definition.getFieldKey(), "type", "NUMBER"));
		} catch (DateTimeParseException e) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_TYPE_MISMATCH,
					"Custom field '" + definition.getFieldKey()
							+ "' expects an ISO-8601 DATE value (yyyy-MM-dd), got: " + rawValue,
					Map.of("field", definition.getFieldKey(), "type", "DATE"));
		}
	}

	private void validateNumber(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		BigDecimal value = new BigDecimal(rawValue);
		if (rule.getMinValue() != null && value.compareTo(rule.getMinValue()) < 0) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_BELOW_MINIMUM,
					"Custom field '" + definition.getFieldKey() + "' must be at least "
							+ rule.getMinValue() + ", got: " + rawValue,
					Map.of("field", definition.getFieldKey(), "min", rule.getMinValue()));
		}
		if (rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_ABOVE_MAXIMUM,
					"Custom field '" + definition.getFieldKey() + "' must be at most "
							+ rule.getMaxValue() + ", got: " + rawValue,
					Map.of("field", definition.getFieldKey(), "max", rule.getMaxValue()));
		}
	}

	private void validateText(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		if (rule.getRegexPattern() != null && !rawValue.matches(rule.getRegexPattern())) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_PATTERN_MISMATCH,
					"Custom field '" + definition.getFieldKey()
							+ "' does not match the required pattern: " + rawValue,
					Map.of("field", definition.getFieldKey()));
		}
	}

	private void validateOption(CustomFieldDefinition definition, CustomFieldValidationRule rule, String rawValue) {
		if (!rule.optionList().contains(rawValue)) {
			throw new BusinessException(SchoolErrorCode.CUSTOM_FIELD_OPTION_INVALID,
					"Custom field '" + definition.getFieldKey()
							+ "' does not allow value '" + rawValue + "' — allowed: " + rule.optionList(),
					Map.of("field", definition.getFieldKey(), "allowed", rule.optionList()));
		}
	}
}

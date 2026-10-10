package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.customfield.model.CustomFieldDefinition;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import com.altafjava.school.domain.customfield.model.CustomFieldType;
import com.altafjava.school.domain.customfield.model.CustomFieldValidationRule;
import com.altafjava.school.domain.customfield.model.CustomFieldVisibilityCondition;
import com.altafjava.school.domain.customfield.repository.CustomFieldDefinitionRepository;

// Tenant-admin CRUD for the custom-field "schema" — same shape as LeaveTypeService/
// CertificateTemplateService. Field *values* are handled separately by CustomFieldValueService.
@Service
public class CustomFieldDefinitionService {

	private final CustomFieldDefinitionRepository customFieldDefinitionRepository;

	public CustomFieldDefinitionService(CustomFieldDefinitionRepository customFieldDefinitionRepository) {
		this.customFieldDefinitionRepository = customFieldDefinitionRepository;
	}

	@Transactional(readOnly = true)
	public Page<CustomFieldDefinition> list(CustomFieldEntityType entityType, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return entityType != null
				? customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, entityType, pageable)
				: customFieldDefinitionRepository.findAllByTenantId(tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public List<CustomFieldDefinition> listActive(CustomFieldEntityType entityType) {
		return customFieldDefinitionRepository.findAllByTenantIdAndEntityTypeAndActiveTrueOrderByDisplayOrderAsc(
				TenantContext.getCurrentTenantId(), entityType);
	}

	@Transactional(readOnly = true)
	public CustomFieldDefinition findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return customFieldDefinitionRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Custom field definition not found: " + publicId));
	}

	@Transactional
	public CustomFieldDefinition create(CustomFieldEntityType entityType, String fieldKey, String label,
			CustomFieldType fieldType, boolean required, CustomFieldValidationRule validationRule, int displayOrder,
			String displayGroup, int displayGroupOrder, CustomFieldVisibilityCondition visibilityCondition) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (customFieldDefinitionRepository.existsByTenantIdAndEntityTypeAndFieldKey(tenantId, entityType,
				fieldKey)) {
			throw new BusinessException(
					"Custom field already defined for " + entityType + ": " + fieldKey);
		}
		requireOptionsForSelectTypes(fieldType, validationRule);
		CustomFieldDefinition definition = CustomFieldDefinition.create(entityType, fieldKey, label, fieldType,
				required);
		definition.updateValidationRule(validationRule);
		definition.updateVisibilityCondition(visibilityCondition);
		definition.reorder(displayOrder, displayGroup, displayGroupOrder);
		return customFieldDefinitionRepository.save(definition);
	}

	@Transactional
	public CustomFieldDefinition updateDetails(String publicId, String label, CustomFieldType fieldType,
			boolean required, CustomFieldValidationRule validationRule, int displayOrder, String displayGroup,
			int displayGroupOrder, CustomFieldVisibilityCondition visibilityCondition,
			ExpectedVersion expectedVersion) {
		requireOptionsForSelectTypes(fieldType, validationRule);
		CustomFieldDefinition definition = findByPublicId(publicId);
		expectedVersion.verify(definition);
		definition.updateDetails(label, fieldType, required);
		definition.updateValidationRule(validationRule);
		definition.updateVisibilityCondition(visibilityCondition);
		definition.reorder(displayOrder, displayGroup, displayGroupOrder);
		return customFieldDefinitionRepository.save(definition);
	}

	private void requireOptionsForSelectTypes(CustomFieldType fieldType, CustomFieldValidationRule validationRule) {
		boolean needsOptions = fieldType == CustomFieldType.SELECT || fieldType == CustomFieldType.MULTI_SELECT;
		boolean hasOptions = validationRule != null && !validationRule.optionList().isEmpty();
		if (needsOptions && !hasOptions) {
			throw new BusinessException(fieldType + " custom fields require at least one option");
		}
	}

	@Transactional
	public CustomFieldDefinition activate(String publicId) {
		CustomFieldDefinition definition = findByPublicId(publicId);
		definition.activate();
		return customFieldDefinitionRepository.save(definition);
	}

	@Transactional
	public CustomFieldDefinition deactivate(String publicId) {
		CustomFieldDefinition definition = findByPublicId(publicId);
		definition.deactivate();
		return customFieldDefinitionRepository.save(definition);
	}
}

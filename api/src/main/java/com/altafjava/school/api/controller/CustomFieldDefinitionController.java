package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.CustomFieldDefinitionApi;
import com.altafjava.school.api.dto.request.CreateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.response.CustomFieldDefinitionResponse;
import com.altafjava.school.api.mapper.CustomFieldDefinitionMapper;
import com.altafjava.school.api.mapper.CustomFieldValidationRuleMapper;
import com.altafjava.school.api.mapper.CustomFieldVisibilityConditionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CustomFieldDefinitionService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

// Tenant-admin-only: this is the "schema" a tenant admin defines for their dynamic/custom fields.
@RestController
@RequestMapping("/api/v1/custom-field-definitions")
public class CustomFieldDefinitionController implements CustomFieldDefinitionApi {

	private final CustomFieldDefinitionService customFieldDefinitionService;
	private final CustomFieldDefinitionMapper customFieldDefinitionMapper;
	private final CustomFieldValidationRuleMapper customFieldValidationRuleMapper;
	private final CustomFieldVisibilityConditionMapper customFieldVisibilityConditionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CustomFieldDefinitionController(CustomFieldDefinitionService customFieldDefinitionService,
			CustomFieldDefinitionMapper customFieldDefinitionMapper,
			CustomFieldValidationRuleMapper customFieldValidationRuleMapper,
			CustomFieldVisibilityConditionMapper customFieldVisibilityConditionMapper,
			SpringDataPageableResolver pageableResolver) {
		this.customFieldDefinitionService = customFieldDefinitionService;
		this.customFieldDefinitionMapper = customFieldDefinitionMapper;
		this.customFieldValidationRuleMapper = customFieldValidationRuleMapper;
		this.customFieldVisibilityConditionMapper = customFieldVisibilityConditionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CustomFieldDefinitionResponse>> list(
			@RequestParam(required = false) CustomFieldEntityType entityType,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(customFieldDefinitionService.list(entityType, pageableResolver.resolve(page, size))
						.map(customFieldDefinitionMapper::toResponse)));
	}

	@Override
	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<List<CustomFieldDefinitionResponse>> listActive(@RequestParam CustomFieldEntityType entityType) {
		return ApiResponse.success(customFieldDefinitionService.listActive(entityType).stream()
				.map(customFieldDefinitionMapper::toResponse)
				.toList());
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<CustomFieldDefinitionResponse> get(@PathVariable String publicId) {
		return ApiResponse
				.success(customFieldDefinitionMapper.toResponse(customFieldDefinitionService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<CustomFieldDefinitionResponse> create(
			@Valid @RequestBody CreateCustomFieldDefinitionRequest request) {
		return ApiResponse.success(
				customFieldDefinitionMapper.toResponse(customFieldDefinitionService.create(request.entityType(),
						request.fieldKey(), request.label(), request.fieldType(), request.required(),
						customFieldValidationRuleMapper.toDomain(request.validationRule()), request.displayOrder(),
						request.displayGroup(), request.displayGroupOrder(),
						customFieldVisibilityConditionMapper.toDomain(request.visibilityCondition()))));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<CustomFieldDefinitionResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCustomFieldDefinitionRequest request) {
		return ApiResponse
				.success(customFieldDefinitionMapper.toResponse(customFieldDefinitionService.updateDetails(publicId,
						request.label(), request.fieldType(), request.required(),
						customFieldValidationRuleMapper.toDomain(request.validationRule()), request.displayOrder(),
						request.displayGroup(), request.displayGroupOrder(),
						customFieldVisibilityConditionMapper.toDomain(request.visibilityCondition()),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/activate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<CustomFieldDefinitionResponse> activate(@PathVariable String publicId) {
		return ApiResponse
				.success(customFieldDefinitionMapper.toResponse(customFieldDefinitionService.activate(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_DEFINITION_MANAGE')")
	public ApiResponse<CustomFieldDefinitionResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse
				.success(customFieldDefinitionMapper.toResponse(customFieldDefinitionService.deactivate(publicId)));
	}
}

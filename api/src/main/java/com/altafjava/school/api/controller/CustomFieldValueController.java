package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.school.api.controller.api.CustomFieldValueApi;
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.api.dto.response.FieldGroupResponse;
import com.altafjava.school.api.mapper.CustomFieldValueMapper;
import com.altafjava.school.api.support.CustomFieldEntityResolver;
import com.altafjava.school.application.service.CustomFieldValueService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/custom-fields/{entityType}/{publicId}")
@RequiredArgsConstructor
public class CustomFieldValueController implements CustomFieldValueApi {

	private final CustomFieldEntityResolver entityResolver;
	private final CustomFieldValueService customFieldValueService;
	private final CustomFieldValueMapper customFieldValueMapper;

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public ApiResponse<List<CustomFieldValueResponse>> get(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId) {
		Long entityId = entityResolver.resolveId(entityType, publicId);
		return ApiResponse.success(
				customFieldValueMapper.toResponseList(customFieldValueService.getAllValues(entityType, entityId)));
	}

	@Override
	@GetMapping("/grouped")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_READ')")
	public ApiResponse<List<FieldGroupResponse>> getGrouped(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId) {
		Long entityId = entityResolver.resolveId(entityType, publicId);
		return ApiResponse.success(customFieldValueMapper
				.toGroupResponseList(customFieldValueService.getGroupedValues(entityType, entityId)));
	}

	@Override
	@PutMapping
	@LastWriteWins("Custom field values are stored as attributes, not on a versioned record")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CUSTOM_FIELD_VALUE_WRITE')")
	public ApiResponse<List<CustomFieldValueResponse>> set(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId, @Valid @RequestBody SetCustomFieldValuesRequest request) {
		Long entityId = entityResolver.resolveId(entityType, publicId);
		for (Map.Entry<String, String> entry : request.values().entrySet()) {
			customFieldValueService.setValue(entityType, entityId, entry.getKey(), entry.getValue());
		}
		return ApiResponse.success(
				customFieldValueMapper.toResponseList(customFieldValueService.getAllValues(entityType, entityId)));
	}
}

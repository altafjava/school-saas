package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdateCustomFieldDefinitionRequest;
import com.altafjava.school.api.dto.response.CustomFieldDefinitionResponse;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Custom Field Definition", description = "Per-tenant EAV extension mechanism: lets a tenant add its "
		+ "own fields (e.g. a board-specific student attribute) to Student, Teacher, Guardian, Admission, and other "
		+ "entities without a schema change. Values are managed separately per entity type's own CustomField endpoint.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CustomFieldDefinitionApi {

	@Operation(summary = "List", description = "Lists custom field definitions, optionally filtered to one entity type "
			+ "(Student, Teacher, Guardian, Admission, ...), including inactive ones.")
	public ApiResponse<com.altafjava.platform.core.model.Page<CustomFieldDefinitionResponse>> list(
			@RequestParam(required = false) CustomFieldEntityType entityType,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List active", description = "Lists only active definitions for one entity type — the set a create/edit form for "
			+ "that entity should render.")
	public ApiResponse<List<CustomFieldDefinitionResponse>> listActive(@RequestParam CustomFieldEntityType entityType);

	@Operation(summary = "Get")
	public ApiResponse<CustomFieldDefinitionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", description = "Defines a new custom field for one entity type — its key, label, data type, and "
			+ "whether it is required.")
	public ApiResponse<CustomFieldDefinitionResponse> create(
			@Valid @RequestBody CreateCustomFieldDefinitionRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<CustomFieldDefinitionResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCustomFieldDefinitionRequest request);

	@Operation(summary = "Activate")
	public ApiResponse<CustomFieldDefinitionResponse> activate(@PathVariable String publicId);

	@Operation(summary = "Deactivate")
	public ApiResponse<CustomFieldDefinitionResponse> deactivate(@PathVariable String publicId);
}

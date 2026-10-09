package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.SetCustomFieldValuesRequest;
import com.altafjava.school.api.dto.response.CustomFieldValueResponse;
import com.altafjava.school.api.dto.response.FieldGroupResponse;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Custom Field Values", description = "Tenant-defined field values on any entity that supports them; the entityType path segment selects which.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CustomFieldValueApi {

	@Operation(summary = "Get", description = "The entity's custom field values, one per active definition for its type.")
	ApiResponse<List<CustomFieldValueResponse>> get(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId);

	@Operation(summary = "Get grouped", description = "The same values grouped by displayGroup, ordered, each with its visibility resolved — render it as it comes.")
	ApiResponse<List<FieldGroupResponse>> getGrouped(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId);

	@Operation(summary = "Set", description = "Sets the given values; keys are field keys. Each value is validated against its definition.")
	ApiResponse<List<CustomFieldValueResponse>> set(@PathVariable CustomFieldEntityType entityType,
			@PathVariable String publicId, @Valid @RequestBody SetCustomFieldValuesRequest request);
}

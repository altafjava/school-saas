package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.UpdateGuardianRegistrationSettingsRequest;
import com.altafjava.school.api.dto.response.GuardianRegistrationSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Guardian Registration Settings", description = "APIs for managing Guardian Registration Settings operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface GuardianRegistrationSettingsApi {

	@Operation(summary = "Get")
	public ApiResponse<GuardianRegistrationSettingsResponse> get();

	@Operation(summary = "Update")
	public ApiResponse<GuardianRegistrationSettingsResponse> update(
			@Valid @RequestBody UpdateGuardianRegistrationSettingsRequest request);
}

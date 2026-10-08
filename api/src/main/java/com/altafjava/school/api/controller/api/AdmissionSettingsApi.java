package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.UpdateAdmissionSettingsRequest;
import com.altafjava.school.api.dto.response.AdmissionSettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admission Settings", description = "School-wide admission settings, such as the application fee.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AdmissionSettingsApi {

	@Operation(summary = "Get", operationId = "admissionsettings_get")
	ApiResponse<AdmissionSettingsResponse> get();

	@Operation(summary = "Update", operationId = "admissionsettings_update")
	ApiResponse<AdmissionSettingsResponse> update(@Valid @RequestBody UpdateAdmissionSettingsRequest request);
}

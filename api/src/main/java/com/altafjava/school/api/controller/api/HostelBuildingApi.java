package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateHostelBuildingRequest;
import com.altafjava.school.api.dto.request.UpdateHostelBuildingRequest;
import com.altafjava.school.api.dto.response.HostelBuildingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Hostel Building", description = "APIs for managing Hostel Building operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface HostelBuildingApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<HostelBuildingResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<HostelBuildingResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<HostelBuildingResponse> create(@Valid @RequestBody CreateHostelBuildingRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<HostelBuildingResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateHostelBuildingRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<HostelBuildingResponse> deactivate(@PathVariable String publicId);
}

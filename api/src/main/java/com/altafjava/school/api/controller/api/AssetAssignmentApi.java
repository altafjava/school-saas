package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignAssetRequest;
import com.altafjava.school.api.dto.request.ReturnAssetRequest;
import com.altafjava.school.api.dto.response.AssetAssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Asset Assignment", description = "APIs for managing Asset Assignment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AssetAssignmentApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssetAssignmentResponse>> list(
			@PathVariable String assetPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Assign")
	public ApiResponse<AssetAssignmentResponse> assign(@PathVariable String assetPublicId,
			@Valid @RequestBody AssignAssetRequest request);

	@Operation(summary = "Mark returned")
	public ApiResponse<AssetAssignmentResponse> markReturned(@PathVariable String assetPublicId,
			@PathVariable String assignmentPublicId, @Valid @RequestBody ReturnAssetRequest request);
}

package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateVehicleRequest;
import com.altafjava.school.api.dto.request.UpdateVehicleRequest;
import com.altafjava.school.api.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicle", description = "APIs for managing Vehicle operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface VehicleApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<VehicleResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<VehicleResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<VehicleResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateVehicleRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<VehicleResponse> deactivate(@PathVariable String publicId);
}

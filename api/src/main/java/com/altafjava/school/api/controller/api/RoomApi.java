package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateRoomRequest;
import com.altafjava.school.api.dto.request.UpdateRoomRequest;
import com.altafjava.school.api.dto.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Room", description = "APIs for managing Room operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface RoomApi {

	@Operation(summary = "List for building")
	public ApiResponse<com.altafjava.platform.core.model.Page<RoomResponse>> listForBuilding(
			@RequestParam String hostelBuildingPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<RoomResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<RoomResponse> create(
			@RequestParam String hostelBuildingPublicId,
			@Valid @RequestBody CreateRoomRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<RoomResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateRoomRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<RoomResponse> deactivate(@PathVariable String publicId);
}

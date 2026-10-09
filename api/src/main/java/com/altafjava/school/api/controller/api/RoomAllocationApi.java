package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AllocateRoomRequest;
import com.altafjava.school.api.dto.request.VacateRoomAllocationRequest;
import com.altafjava.school.api.dto.response.RoomAllocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Room Allocation", description = "APIs for managing Room Allocation operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface RoomAllocationApi {

	@Operation(summary = "List for room")
	public ApiResponse<com.altafjava.platform.core.model.Page<RoomAllocationResponse>> listForRoom(
			@RequestParam String roomPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Allocate")
	public ApiResponse<RoomAllocationResponse> allocate(@Valid @RequestBody AllocateRoomRequest request);

	@Operation(summary = "Vacate")
	public ApiResponse<RoomAllocationResponse> vacate(@PathVariable String publicId,
			@Valid @RequestBody VacateRoomAllocationRequest request);
}

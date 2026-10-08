package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateVenueRequest;
import com.altafjava.school.api.dto.request.UpdateVenueRequest;
import com.altafjava.school.api.dto.response.VenueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Venue", description = "APIs for managing Venue operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface VenueApi {

	@Operation(summary = "List", operationId = "venue_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<VenueResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "venue_get")
	public ApiResponse<VenueResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "venue_create", description = "Registers a physical space (classroom, laboratory, hall, ...) that timetable slots can be held in.")
	public ApiResponse<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request);

	@Operation(summary = "Update", operationId = "venue_update")
	public ApiResponse<VenueResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdateVenueRequest request);

	@Operation(summary = "Deactivate", operationId = "venue_deactivate", description = "A deactivated venue cannot be given to new timetable slots; existing bookings stay.")
	public ApiResponse<VenueResponse> deactivate(@PathVariable String publicId);

	@Operation(summary = "Activate", operationId = "venue_activate")
	public ApiResponse<VenueResponse> activate(@PathVariable String publicId);
}

package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AddRouteStopRequest;
import com.altafjava.school.api.dto.request.CreateRouteRequest;
import com.altafjava.school.api.dto.request.UpdateRouteRequest;
import com.altafjava.school.api.dto.response.RouteResponse;
import com.altafjava.school.api.dto.response.RouteStopResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Route", description = "APIs for managing Route operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface RouteApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<RouteResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<RouteResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<RouteResponse> create(@Valid @RequestBody CreateRouteRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<RouteResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateRouteRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<RouteResponse> deactivate(@PathVariable String publicId);

	@Operation(summary = "List stops")
	public ApiResponse<List<RouteStopResponse>> listStops(@PathVariable String publicId);

	@Operation(summary = "Add stop")
	public ApiResponse<RouteStopResponse> addStop(@PathVariable String publicId,
			@Valid @RequestBody AddRouteStopRequest request);
}

package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignTransportRequest;
import com.altafjava.school.api.dto.request.EndTransportAssignmentRequest;
import com.altafjava.school.api.dto.response.TransportAssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transport Assignment", description = "APIs for managing Transport Assignment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface TransportAssignmentApi {

	@Operation(summary = "List for route")
	public ApiResponse<com.altafjava.platform.core.model.Page<TransportAssignmentResponse>> listForRoute(
			@RequestParam String routePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Assign")
	public ApiResponse<TransportAssignmentResponse> assign(@Valid @RequestBody AssignTransportRequest request);

	@Operation(summary = "End")
	public ApiResponse<TransportAssignmentResponse> end(@PathVariable String publicId,
			@Valid @RequestBody EndTransportAssignmentRequest request);
}

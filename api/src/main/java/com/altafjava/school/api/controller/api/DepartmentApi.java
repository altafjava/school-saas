package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignHeadEmployeeRequest;
import com.altafjava.school.api.dto.request.CreateDepartmentRequest;
import com.altafjava.school.api.dto.request.UpdateDepartmentRequest;
import com.altafjava.school.api.dto.response.DepartmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Department", description = "APIs for managing Department operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface DepartmentApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<DepartmentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<DepartmentResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<DepartmentResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateDepartmentRequest request);

	@Operation(summary = "Assign head employee")
	public ApiResponse<DepartmentResponse> assignHeadEmployee(@PathVariable String publicId,
			@Valid @RequestBody AssignHeadEmployeeRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<DepartmentResponse> deactivate(@PathVariable String publicId);
}

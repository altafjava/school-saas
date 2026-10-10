package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateAcademicYearRequest;
import com.altafjava.school.api.dto.response.AcademicYearResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Academic Year", description = "APIs for managing Academic Year operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AcademicYearApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<AcademicYearResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<AcademicYearResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<AcademicYearResponse> create(@Valid @RequestBody CreateAcademicYearRequest request);
}

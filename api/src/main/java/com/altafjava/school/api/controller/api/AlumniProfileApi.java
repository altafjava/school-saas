package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateAlumniProfileRequest;
import com.altafjava.school.api.dto.request.UpdateAlumniContactInfoRequest;
import com.altafjava.school.api.dto.response.AlumniProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Alumni Profile", description = "APIs for managing Alumni Profile operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AlumniProfileApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<AlumniProfileResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<AlumniProfileResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<AlumniProfileResponse> create(@Valid @RequestBody CreateAlumniProfileRequest request);

	@Operation(summary = "Update contact info")
	public ApiResponse<AlumniProfileResponse> updateContactInfo(@PathVariable String publicId,
			@Valid @RequestBody UpdateAlumniContactInfoRequest request);

	@Operation(summary = "Activate")
	public ApiResponse<AlumniProfileResponse> activate(@PathVariable String publicId);

	@Operation(summary = "Deactivate")
	public ApiResponse<AlumniProfileResponse> deactivate(@PathVariable String publicId);
}

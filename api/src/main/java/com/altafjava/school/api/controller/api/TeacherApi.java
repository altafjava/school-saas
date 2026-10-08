package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateTeacherRequest;
import com.altafjava.school.api.dto.response.TeacherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Teacher", description = "APIs for managing Teacher operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface TeacherApi {

	@Operation(summary = "List", operationId = "teacher_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<TeacherResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "teacher_get")
	public ApiResponse<TeacherResponse> get(@PathVariable String publicId);

	@Operation(summary = "Hire", operationId = "teacher_hire")
	public ApiResponse<TeacherResponse> hire(@Valid @RequestBody CreateTeacherRequest request);
}

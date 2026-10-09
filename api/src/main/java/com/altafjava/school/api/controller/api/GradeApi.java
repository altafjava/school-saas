package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CorrectGradeRequest;
import com.altafjava.school.api.dto.request.RecordGradeRequest;
import com.altafjava.school.api.dto.response.GradeCorrectionResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Grade", description = "APIs for managing Grade operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface GradeApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<GradeResponse> get(@PathVariable String publicId);

	@Operation(summary = "Record")
	public ApiResponse<GradeResponse> record(@Valid @RequestBody RecordGradeRequest request);

	@Operation(summary = "Correct")
	public ApiResponse<GradeResponse> correct(@PathVariable String publicId,
			@Valid @RequestBody CorrectGradeRequest request);

	@Operation(summary = "List corrections")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeCorrectionResponse>> listCorrections(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);
}

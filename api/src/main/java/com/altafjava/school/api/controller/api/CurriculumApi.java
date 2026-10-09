package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignGradingScaleRequest;
import com.altafjava.school.api.dto.request.CreateCurriculumRequest;
import com.altafjava.school.api.dto.request.UpdateCurriculumRequest;
import com.altafjava.school.api.dto.response.CurriculumResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Curriculum", description = "APIs for managing Curriculum operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CurriculumApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<CurriculumResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<CurriculumResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<CurriculumResponse> create(@Valid @RequestBody CreateCurriculumRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<CurriculumResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCurriculumRequest request);

	@Operation(summary = "Assign grading scale")
	public ApiResponse<CurriculumResponse> assignGradingScale(@PathVariable String publicId,
			@Valid @RequestBody AssignGradingScaleRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<CurriculumResponse> deactivate(@PathVariable String publicId);
}

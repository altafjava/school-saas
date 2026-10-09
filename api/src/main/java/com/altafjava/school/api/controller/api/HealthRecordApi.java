package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.UpsertHealthRecordRequest;
import com.altafjava.school.api.dto.response.HealthRecordCorrectionResponse;
import com.altafjava.school.api.dto.response.HealthRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health Record", description = "APIs for managing Health Record operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface HealthRecordApi {

	@Operation(summary = "Get by student")
	public ApiResponse<HealthRecordResponse> getByStudent(@PathVariable String studentPublicId);

	@Operation(summary = "Upsert")
	public ApiResponse<HealthRecordResponse> upsert(@PathVariable String studentPublicId,
			@Valid @RequestBody UpsertHealthRecordRequest request);

	@Operation(summary = "List corrections")
	public ApiResponse<com.altafjava.platform.core.model.Page<HealthRecordCorrectionResponse>> listCorrections(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);
}

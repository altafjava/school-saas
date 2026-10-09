package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ConfigureReportCardTemplateRequest;
import com.altafjava.school.api.dto.response.ReportCardTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Report Card Template", description = "APIs for managing Report Card Template operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface ReportCardTemplateApi {

	@Operation(summary = "Get")
	public ApiResponse<ReportCardTemplateResponse> get();

	@Operation(summary = "Configure")
	public ApiResponse<ReportCardTemplateResponse> configure(
			@Valid @RequestBody ConfigureReportCardTemplateRequest request);
}

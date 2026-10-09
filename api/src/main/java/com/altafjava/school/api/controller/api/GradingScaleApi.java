package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateGradingScaleRequest;
import com.altafjava.school.api.dto.request.UpdateGradingScaleThresholdsRequest;
import com.altafjava.school.api.dto.response.GradingScaleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Grading Scale", description = "APIs for managing Grading Scale operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface GradingScaleApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradingScaleResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<GradingScaleResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", description = "Defines a new tenant grading scale (its score-to-grade thresholds) — lets each tenant "
			+ "use its own board/curriculum's grading system rather than a fixed one.")
	public ApiResponse<GradingScaleResponse> create(@Valid @RequestBody CreateGradingScaleRequest request);

	@Operation(summary = "Update thresholds")
	public ApiResponse<GradingScaleResponse> updateThresholds(@PathVariable String publicId,
			@Valid @RequestBody UpdateGradingScaleThresholdsRequest request);

	@Operation(summary = "Mark as default", description = "Sets this scale as the tenant's default — the scale used when grading doesn't specify "
			+ "one explicitly. Only one scale can be default per tenant.")
	public ApiResponse<GradingScaleResponse> markAsDefault(@PathVariable String publicId);

	@Operation(summary = "Deactivate")
	public ApiResponse<GradingScaleResponse> deactivate(@PathVariable String publicId);
}

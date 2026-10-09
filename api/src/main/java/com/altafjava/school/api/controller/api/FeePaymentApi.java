package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.RecordFeePaymentRequest;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Payment", description = "APIs for managing Fee Payment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface FeePaymentApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeePaymentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<FeePaymentResponse> get(@PathVariable String publicId);

	@Operation(summary = "Record")
	public ApiResponse<FeePaymentResponse> record(@Valid @RequestBody RecordFeePaymentRequest request);
}

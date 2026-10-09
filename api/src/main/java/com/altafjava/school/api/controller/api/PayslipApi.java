package com.altafjava.school.api.controller.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.PayslipResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Payslip", description = "APIs for managing Payslip operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface PayslipApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<PayslipResponse>> list(
			@RequestParam(required = false) String employeePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<PayslipResponse> get(@PathVariable String publicId);

	@Operation(summary = "Finalize payslip")
	public ApiResponse<PayslipResponse> finalizePayslip(@PathVariable String publicId);

	@Operation(summary = "Disburse")
	public ApiResponse<PayslipResponse> disburse(@PathVariable String publicId);
}

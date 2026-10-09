package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateSalaryStructureRequest;
import com.altafjava.school.api.dto.request.SupersedeSalaryStructureRequest;
import com.altafjava.school.api.dto.response.SalaryStructureResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Salary Structure", description = "APIs for managing Salary Structure operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface SalaryStructureApi {

	@Operation(summary = "List for employee")
	public ApiResponse<com.altafjava.platform.core.model.Page<SalaryStructureResponse>> listForEmployee(
			@RequestParam String employeePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<SalaryStructureResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", description = "Creates a employee's salary structure from a tenant-defined set of pay components "
			+ "(see Pay Component Definition) — every component code must exist and be active in the "
			+ "tenant's catalog; there is no fixed Basic/HRA/Transport shape, so this works for any region's "
			+ "pay-component naming.")
	public ApiResponse<SalaryStructureResponse> create(@Valid @RequestBody CreateSalaryStructureRequest request);

	@Operation(summary = "Supersede", description = "Replaces the current structure with a new one effective from a given date, preserving "
			+ "history — the prior structure is never mutated or deleted, only superseded.")
	public ApiResponse<SalaryStructureResponse> supersede(@PathVariable String publicId,
			@Valid @RequestBody SupersedeSalaryStructureRequest request);
}

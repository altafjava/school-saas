package com.altafjava.school.api.controller.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "APIs for managing Dashboard operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface DashboardApi {

	@Operation(summary = "Principal")
	public ApiResponse<List<Map<String, Object>>> principal();

	@Operation(summary = "Finance")
	public ApiResponse<List<Map<String, Object>>> finance();

	@Operation(summary = "Hr")
	public ApiResponse<List<Map<String, Object>>> hr();

	@Operation(summary = "Academic")
	public ApiResponse<List<Map<String, Object>>> academic();

	@Operation(summary = "Principal trends")
	public ApiResponse<List<Map<String, Object>>> principalTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Academic trends")
	public ApiResponse<List<Map<String, Object>>> academicTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Finance trends")
	public ApiResponse<List<Map<String, Object>>> financeTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Hr trends")
	public ApiResponse<List<Map<String, Object>>> hrTrends(@RequestParam(required = false) Integer periods);
}

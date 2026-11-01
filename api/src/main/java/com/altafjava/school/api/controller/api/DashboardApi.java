package com.altafjava.school.api.controller.api;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.dashboard.AcademicDashboardRow;
import com.altafjava.school.api.dto.response.dashboard.AttendanceTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.FeeCollectionTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.FinanceDashboardResponse;
import com.altafjava.school.api.dto.response.dashboard.HrDashboardResponse;
import com.altafjava.school.api.dto.response.dashboard.LeaveUtilizationTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.PrincipalDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Dashboard", description = "APIs for managing Dashboard operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface DashboardApi {

	@Operation(summary = "Principal")
	public ApiResponse<List<PrincipalDashboardResponse>> principal();

	@Operation(summary = "Finance")
	public ApiResponse<List<FinanceDashboardResponse>> finance();

	@Operation(summary = "Hr")
	public ApiResponse<List<HrDashboardResponse>> hr();

	@Operation(summary = "Academic")
	public ApiResponse<List<AcademicDashboardRow>> academic();

	@Operation(summary = "Principal trends")
	public ApiResponse<List<AttendanceTrendPoint>> principalTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Academic trends")
	public ApiResponse<List<AttendanceTrendPoint>> academicTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Finance trends")
	public ApiResponse<List<FeeCollectionTrendPoint>> financeTrends(@RequestParam(required = false) Integer periods);

	@Operation(summary = "Hr trends")
	public ApiResponse<List<LeaveUtilizationTrendPoint>> hrTrends(@RequestParam(required = false) Integer periods);
}

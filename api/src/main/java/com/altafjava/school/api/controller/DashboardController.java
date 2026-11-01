package com.altafjava.school.api.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.DashboardApi;
import com.altafjava.school.api.dto.response.dashboard.AcademicDashboardRow;
import com.altafjava.school.api.dto.response.dashboard.AttendanceTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.FeeCollectionTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.FinanceDashboardResponse;
import com.altafjava.school.api.dto.response.dashboard.HrDashboardResponse;
import com.altafjava.school.api.dto.response.dashboard.LeaveUtilizationTrendPoint;
import com.altafjava.school.api.dto.response.dashboard.PrincipalDashboardResponse;
import com.altafjava.school.application.dashboard.AcademicDashboardDataProvider;
import com.altafjava.school.application.dashboard.AttendanceTrendDataProvider;
import com.altafjava.school.application.dashboard.FeeCollectionTrendDataProvider;
import com.altafjava.school.application.dashboard.FinanceDashboardDataProvider;
import com.altafjava.school.application.dashboard.HrDashboardDataProvider;
import com.altafjava.school.application.dashboard.LeaveUtilizationTrendDataProvider;
import com.altafjava.school.application.dashboard.PrincipalDashboardDataProvider;
import tools.jackson.databind.ObjectMapper;

/**
 * Live JSON view of each role-scoped dashboard — calls the same {@code ReportDataProvider} bean
 * directly (synchronously) that {@code ServiceCallReportDataFetcher} also calls when a tenant
 * admin exports the identical data as a CSV/Excel/PDF report via the seeded {@code ReportDefinition}
 * (see {@code SchoolTenantProvisioningListener}) — one aggregation implementation, two consumption
 * paths, since a live dashboard wants an immediate JSON response, not the async
 * execute-then-poll-then-download flow the generic report pipeline is built for.
 *
 * <p>
 * {@code /trends} endpoints (Phase 4) are the same pattern applied to historical, multi-row data
 * instead of a single current-state summary row — {@code periods} controls how many trailing
 * weeks/months to return (defaults live on each provider).
 */
@RestController
@RequestMapping("/api/v1/dashboards")
public class DashboardController implements DashboardApi {

	private final PrincipalDashboardDataProvider principalDashboardDataProvider;
	private final FinanceDashboardDataProvider financeDashboardDataProvider;
	private final HrDashboardDataProvider hrDashboardDataProvider;
	private final AcademicDashboardDataProvider academicDashboardDataProvider;
	private final AttendanceTrendDataProvider attendanceTrendDataProvider;
	private final FeeCollectionTrendDataProvider feeCollectionTrendDataProvider;
	private final LeaveUtilizationTrendDataProvider leaveUtilizationTrendDataProvider;

	private final ObjectMapper objectMapper;

	public DashboardController(ObjectMapper objectMapper,
			PrincipalDashboardDataProvider principalDashboardDataProvider,
			FinanceDashboardDataProvider financeDashboardDataProvider,
			HrDashboardDataProvider hrDashboardDataProvider,
			AcademicDashboardDataProvider academicDashboardDataProvider,
			AttendanceTrendDataProvider attendanceTrendDataProvider,
			FeeCollectionTrendDataProvider feeCollectionTrendDataProvider,
			LeaveUtilizationTrendDataProvider leaveUtilizationTrendDataProvider) {
		this.objectMapper = objectMapper;
		this.principalDashboardDataProvider = principalDashboardDataProvider;
		this.financeDashboardDataProvider = financeDashboardDataProvider;
		this.hrDashboardDataProvider = hrDashboardDataProvider;
		this.academicDashboardDataProvider = academicDashboardDataProvider;
		this.attendanceTrendDataProvider = attendanceTrendDataProvider;
		this.feeCollectionTrendDataProvider = feeCollectionTrendDataProvider;
		this.leaveUtilizationTrendDataProvider = leaveUtilizationTrendDataProvider;
	}

	@Override
	@GetMapping("/principal")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_PRINCIPAL_READ')")
	public ApiResponse<List<PrincipalDashboardResponse>> principal() {
		return ApiResponse
				.success(typed(principalDashboardDataProvider.fetchData(Map.of()), PrincipalDashboardResponse.class));
	}

	@Override
	@GetMapping("/finance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_FINANCE_READ')")
	public ApiResponse<List<FinanceDashboardResponse>> finance() {
		return ApiResponse
				.success(typed(financeDashboardDataProvider.fetchData(Map.of()), FinanceDashboardResponse.class));
	}

	@Override
	@GetMapping("/hr")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_HR_READ')")
	public ApiResponse<List<HrDashboardResponse>> hr() {
		return ApiResponse.success(typed(hrDashboardDataProvider.fetchData(Map.of()), HrDashboardResponse.class));
	}

	@Override
	@GetMapping("/academic")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_ACADEMIC_READ')")
	public ApiResponse<List<AcademicDashboardRow>> academic() {
		return ApiResponse
				.success(typed(academicDashboardDataProvider.fetchData(Map.of()), AcademicDashboardRow.class));
	}

	@Override
	@GetMapping("/principal/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_PRINCIPAL_READ')")
	public ApiResponse<List<AttendanceTrendPoint>> principalTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(
				typed(attendanceTrendDataProvider.fetchData(trendParameters(periods)), AttendanceTrendPoint.class));
	}

	@Override
	@GetMapping("/academic/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_ACADEMIC_READ')")
	public ApiResponse<List<AttendanceTrendPoint>> academicTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(
				typed(attendanceTrendDataProvider.fetchData(trendParameters(periods)), AttendanceTrendPoint.class));
	}

	@Override
	@GetMapping("/finance/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_FINANCE_READ')")
	public ApiResponse<List<FeeCollectionTrendPoint>> financeTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(typed(feeCollectionTrendDataProvider.fetchData(trendParameters(periods)),
				FeeCollectionTrendPoint.class));
	}

	@Override
	@GetMapping("/hr/trends")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DASHBOARD_HR_READ')")
	public ApiResponse<List<LeaveUtilizationTrendPoint>> hrTrends(@RequestParam(required = false) Integer periods) {
		return ApiResponse.success(typed(leaveUtilizationTrendDataProvider.fetchData(trendParameters(periods)),
				LeaveUtilizationTrendPoint.class));
	}

	// The providers also feed the report engine, which works on rows; the API contract stays typed.
	private <T> List<T> typed(List<Map<String, Object>> rows, Class<T> type) {
		return rows.stream().map(row -> objectMapper.convertValue(row, type)).toList();
	}

	private Map<String, Object> trendParameters(Integer periods) {
		return periods == null ? Map.of() : Map.of("periods", periods);
	}
}

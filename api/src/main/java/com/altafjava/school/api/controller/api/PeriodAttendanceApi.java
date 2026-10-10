package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.MarkPeriodAttendanceRequest;
import com.altafjava.school.api.dto.response.PeriodAttendanceResponse;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Period Attendance", description = "APIs for managing Period Attendance operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface PeriodAttendanceApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<PeriodAttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) AttendanceStatus status);

	@Operation(summary = "Get")
	public ApiResponse<PeriodAttendanceResponse> get(@PathVariable String publicId);

	@Operation(summary = "Mark")
	public ApiResponse<PeriodAttendanceResponse> mark(@Valid @RequestBody MarkPeriodAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);
}

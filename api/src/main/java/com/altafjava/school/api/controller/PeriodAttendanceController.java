package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.PeriodAttendanceApi;
import com.altafjava.school.api.dto.request.MarkPeriodAttendanceRequest;
import com.altafjava.school.api.dto.response.PeriodAttendanceResponse;
import com.altafjava.school.api.mapper.PeriodAttendanceMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.AttendanceFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.service.PeriodAttendanceService;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;

@RestController
@RequestMapping("/api/v1/period-attendance")
public class PeriodAttendanceController implements PeriodAttendanceApi {

	private final PeriodAttendanceService periodAttendanceService;
	private final PeriodAttendanceMapper periodAttendanceMapper;
	private final SpringDataPageableResolver pageableResolver;

	public PeriodAttendanceController(PeriodAttendanceService periodAttendanceService,
			PeriodAttendanceMapper periodAttendanceMapper, SpringDataPageableResolver pageableResolver) {
		this.periodAttendanceService = periodAttendanceService;
		this.periodAttendanceMapper = periodAttendanceMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_ATTENDANCE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<PeriodAttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) AttendanceStatus status) {
		AttendanceFilter filter = new AttendanceFilter(classroomPublicId, studentPublicId, new DateWindow(from, to),
				status);
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(periodAttendanceService.listAttendance(filter, pageableResolver.resolve(page, size))
						.map(periodAttendanceMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_ATTENDANCE_MANAGE')")
	public ApiResponse<PeriodAttendanceResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(periodAttendanceMapper.toResponse(periodAttendanceService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_ATTENDANCE_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<PeriodAttendanceResponse> mark(@Valid @RequestBody MarkPeriodAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(periodAttendanceMapper.toResponse(periodAttendanceService.mark(
				request.studentPublicId(),
				request.timetableEntryPublicId(),
				request.attendanceDate(),
				request.status(),
				user.getUsername())));
	}
}

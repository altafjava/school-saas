package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.RecordStaffAttendanceRequest;
import com.altafjava.school.api.dto.request.UpdateStaffWorkdayRequest;
import com.altafjava.school.api.dto.response.staffattendance.StaffAttendanceResponse;
import com.altafjava.school.api.dto.response.staffattendance.StaffRegisterEntryResponse;
import com.altafjava.school.api.dto.response.staffattendance.StaffWorkdayResponse;
import com.altafjava.school.api.mapper.StaffAttendanceMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.staffattendance.StaffAttendanceFilter;
import com.altafjava.school.application.staffattendance.StaffAttendanceService;
import com.altafjava.school.application.staffattendance.StaffWorkdaySettingsService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.staffattendance.model.StaffAttendance;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;
import com.altafjava.school.domain.staffattendance.model.StaffWorkday;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Staff attendance", description = "Staff check-in and check-out, the daily register and the workday rules.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/staff-attendance")
@RequiredArgsConstructor
public class StaffAttendanceController {

	private final StaffAttendanceService staffAttendanceService;
	private final StaffWorkdaySettingsService staffWorkdaySettingsService;
	private final StaffAttendanceMapper staffAttendanceMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@PostMapping("/me/check-in")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_SELF_SERVICE)
	@Operation(summary = "Check in", description = "Records the caller's arrival at the school's current time. Refused "
			+ "on a day of approved leave or once the day is recorded.")
	public ApiResponse<StaffAttendanceResponse> checkIn(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(staffAttendanceMapper.toResponse(staffAttendanceService.checkIn(user.getId())));
	}

	@PostMapping("/me/check-out")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_SELF_SERVICE)
	@Operation(summary = "Check out", description = "Records the caller's departure at the school's current time.")
	public ApiResponse<StaffAttendanceResponse> checkOut(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(staffAttendanceMapper.toResponse(staffAttendanceService.checkOut(user.getId())));
	}

	@GetMapping("/me")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_SELF_SERVICE)
	@SortableBy({ "attendanceDate", "status" })
	@Operation(summary = "My attendance")
	public ApiResponse<com.altafjava.platform.core.model.Page<StaffAttendanceResponse>> mine(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) StaffAttendanceStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				staffAttendanceService.ownHistory(user.getId(),
						new StaffAttendanceFilter(null, status, new DateWindow(from, to)),
						pageableResolver.resolve(page, size)),
				staffAttendanceMapper::toResponse)));
	}

	@GetMapping
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_READ)
	@SortableBy({ "attendanceDate", "status" })
	@Operation(summary = "Staff attendance", description = "Recorded days; every filter is optional.")
	public ApiResponse<com.altafjava.platform.core.model.Page<StaffAttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String employeePublicId,
			@RequestParam(required = false) StaffAttendanceStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				staffAttendanceService.list(
						new StaffAttendanceFilter(employeePublicId, status, new DateWindow(from, to)),
						pageableResolver.resolve(page, size)),
				staffAttendanceMapper::toResponse)));
	}

	@GetMapping("/register")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_READ)
	@SortableBy({ "firstName", "lastName", "employeeCode" })
	@Operation(summary = "Daily register", description = "Every active employee's line for the day, including approved "
			+ "leave.")
	public ApiResponse<com.altafjava.platform.core.model.Page<StaffRegisterEntryResponse>> register(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(staffAttendanceService
				.register(date, pageableResolver.resolve(page, size)).map(staffAttendanceMapper::toResponse)));
	}

	@PutMapping("/employees/{employeePublicId}/days/{date}")
	@LastWriteWins("An administrator's record of a day replaces the last one, and is audited")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_MANAGE)
	@Operation(summary = "Record a day", description = "Records or corrects one employee's day. Late, early and half "
			+ "day follow from the times and the school's workday.")
	public ApiResponse<StaffAttendanceResponse> record(@PathVariable String employeePublicId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody RecordStaffAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(staffAttendanceMapper.toResponse(staffAttendanceService.record(employeePublicId, date,
						new StaffAttendance.DayRecord(request.status(), request.checkInAt(), request.checkOutAt(),
								request.note()),
						user.getUsername())));
	}

	@GetMapping("/workday")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_READ)
	@Operation(summary = "Workday")
	public ApiResponse<StaffWorkdayResponse> workday() {
		return ApiResponse.success(staffAttendanceMapper.toResponse(staffWorkdaySettingsService.current()));
	}

	@PutMapping("/workday")
	@LastWriteWins("A single tenant setting, with no versioned record behind it")
	@RequiresPermission(SchoolPermissions.STAFF_ATTENDANCE_MANAGE)
	@Operation(summary = "Update workday")
	public ApiResponse<StaffWorkdayResponse> updateWorkday(@Valid @RequestBody UpdateStaffWorkdayRequest request) {
		return ApiResponse.success(staffAttendanceMapper.toResponse(staffWorkdaySettingsService.update(new StaffWorkday(
				request.start(), request.end(), request.lateGraceMinutes(), request.halfDayMinutes()))));
	}
}

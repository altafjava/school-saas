package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.AttendanceScanApi;
import com.altafjava.school.api.dto.request.ScanAttendanceRequest;
import com.altafjava.school.api.dto.response.AttendanceScanResponse;
import com.altafjava.school.api.mapper.AttendanceScanMapper;
import com.altafjava.school.application.service.AttendanceScanService;

@RestController
@RequestMapping("/api/v1/attendance/scan")
public class AttendanceScanController implements AttendanceScanApi {

	private final AttendanceScanService attendanceScanService;
	private final AttendanceScanMapper attendanceScanMapper;

	public AttendanceScanController(AttendanceScanService attendanceScanService,
			AttendanceScanMapper attendanceScanMapper) {
		this.attendanceScanService = attendanceScanService;
		this.attendanceScanMapper = attendanceScanMapper;
	}

	@Override
	@PostMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public ApiResponse<AttendanceScanResponse> scan(@Valid @RequestBody ScanAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(attendanceScanMapper
				.toResponse(attendanceScanService.scan(request.payload(), user.getUsername())));
	}
}

package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.altafjava.school.api.controller.api.AttendanceApi;
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;
import com.altafjava.school.api.dto.request.UpdateAttendanceStatusRequest;
import com.altafjava.school.api.dto.response.AttendanceCorrectionResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.mapper.AttendanceCorrectionMapper;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AttendanceService;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController implements AttendanceApi {

	private final AttendanceService attendanceService;
	private final AttendanceMapper attendanceMapper;
	private final AttendanceCorrectionMapper attendanceCorrectionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AttendanceController(AttendanceService attendanceService, AttendanceMapper attendanceMapper,
			AttendanceCorrectionMapper attendanceCorrectionMapper, SpringDataPageableResolver pageableResolver) {
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
		this.attendanceCorrectionMapper = attendanceCorrectionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	@SortableBy({ "attendanceDate", "status" })
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(attendanceService.listAttendance(pageableResolver.resolve(page, size))
						.map(attendanceMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	public ApiResponse<AttendanceResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(attendanceMapper.toResponse(attendanceService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	@RequireIdempotencyKey
	public ApiResponse<AttendanceResponse> mark(@Valid @RequestBody MarkAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(attendanceMapper.toResponse(attendanceService.mark(
				request.studentPublicId(),
				request.classroomPublicId(),
				request.attendanceDate(),
				request.status(),
				user.getUsername())));
	}

	@Override
	@PatchMapping("/{publicId}/status")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public ApiResponse<AttendanceResponse> updateStatus(@PathVariable String publicId,
			@Valid @RequestBody UpdateAttendanceStatusRequest request) {
		return ApiResponse
				.success(attendanceMapper.toResponse(attendanceService.updateStatus(publicId, request.status())));
	}

	@Override
	@GetMapping("/{publicId}/corrections")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceCorrectionResponse>> listCorrections(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(attendanceService.listCorrections(publicId, pageableResolver.resolve(page, size))
						.map(attendanceCorrectionMapper::toResponse)));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_ATTENDANCE_WRITE')")
	public ApiResponse<Void> delete(@PathVariable String publicId) {
		attendanceService.delete(publicId);
		return ApiResponse.success(null);
	}
}

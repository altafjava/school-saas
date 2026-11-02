package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.StudentLeaveRequestApi;
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitStudentLeaveRequest;
import com.altafjava.school.api.dto.response.StudentLeaveRequestResponse;
import com.altafjava.school.api.mapper.StudentLeaveRequestMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.StudentLeaveRequestFilter;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.StudentLeaveRequestService;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student-leave-requests")
@RequiredArgsConstructor
public class StudentLeaveRequestController implements StudentLeaveRequestApi {

	private final StudentLeaveRequestService studentLeaveRequestService;
	private final StudentLeaveRequestMapper studentLeaveRequestMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@Override
	@GetMapping
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_SELF_SERVICE, SchoolPermissions.STUDENT_LEAVE_REVIEW,
			SchoolPermissions.STUDENT_LEAVE_MANAGE })
	@SortableBy({ "startDate", "endDate", "status" })
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentLeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) LeaveRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		StudentLeaveRequestFilter filter = new StudentLeaveRequestFilter(studentPublicId, classroomPublicId, status,
				new DateWindow(from, to));
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				studentLeaveRequestService.list(filter, pageableResolver.resolve(page, size)),
				studentLeaveRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_SELF_SERVICE, SchoolPermissions.STUDENT_LEAVE_REVIEW,
			SchoolPermissions.STUDENT_LEAVE_MANAGE })
	public ApiResponse<StudentLeaveRequestResponse> get(@PathVariable String publicId) {
		return ApiResponse
				.success(studentLeaveRequestMapper.toResponse(studentLeaveRequestService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_SELF_SERVICE, SchoolPermissions.STUDENT_LEAVE_REVIEW,
			SchoolPermissions.STUDENT_LEAVE_MANAGE })
	public ApiResponse<StudentLeaveRequestResponse> submit(@Valid @RequestBody SubmitStudentLeaveRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentLeaveRequestMapper.toResponse(studentLeaveRequestService.submit(
				request.studentPublicId(), request.startDate(), request.endDate(), request.reason(),
				request.attachmentFilePublicId(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/approve")
	@Command
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_REVIEW, SchoolPermissions.STUDENT_LEAVE_MANAGE })
	public ApiResponse<StudentLeaveRequestResponse> approve(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(
				studentLeaveRequestMapper.toResponse(studentLeaveRequestService.approve(publicId, user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/reject")
	@Command
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_REVIEW, SchoolPermissions.STUDENT_LEAVE_MANAGE })
	public ApiResponse<StudentLeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentLeaveRequestMapper.toResponse(
				studentLeaveRequestService.reject(publicId, request.rejectionReason(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@Command
	@RequiresPermission({ SchoolPermissions.STUDENT_LEAVE_SELF_SERVICE, SchoolPermissions.STUDENT_LEAVE_REVIEW,
			SchoolPermissions.STUDENT_LEAVE_MANAGE })
	public ApiResponse<StudentLeaveRequestResponse> cancel(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(
				studentLeaveRequestMapper.toResponse(studentLeaveRequestService.cancel(publicId, user.getId())));
	}
}

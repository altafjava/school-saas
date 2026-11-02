package com.altafjava.school.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import com.altafjava.school.api.controller.api.LeaveRequestApi;
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitLeaveRequestRequest;
import com.altafjava.school.api.dto.response.LeaveApprovalResponse;
import com.altafjava.school.api.dto.response.LeaveRequestResponse;
import com.altafjava.school.api.mapper.LeaveApprovalMapper;
import com.altafjava.school.api.mapper.LeaveRequestMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.LeaveRequestFilter;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.LeaveRequestService;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController implements LeaveRequestApi {

	private final LeaveRequestService leaveRequestService;
	private final LeaveRequestMapper leaveRequestMapper;
	private final LeaveApprovalMapper leaveApprovalMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public LeaveRequestController(LeaveRequestService leaveRequestService, LeaveRequestMapper leaveRequestMapper,
			LeaveApprovalMapper leaveApprovalMapper, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.leaveRequestService = leaveRequestService;
		this.leaveRequestMapper = leaveRequestMapper;
		this.leaveApprovalMapper = leaveApprovalMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.LEAVE_REQUEST_MANAGE)
	@SortableBy({ "startDate", "endDate", "status", "daysRequested" })
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String employeePublicId,
			@RequestParam(required = false) String leaveTypePublicId,
			@RequestParam(required = false) LeaveRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		LeaveRequestFilter filter = new LeaveRequestFilter(employeePublicId, leaveTypePublicId, status,
				new DateWindow(from, to));
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(
						leaveRequestService.listAll(filter, pageableResolver.resolve(page, size)),
						leaveRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/my")
	@RequiresPermission(SchoolPermissions.LEAVE_SELF_SERVICE)
	@SortableBy({ "startDate", "endDate", "status", "daysRequested" })
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String leaveTypePublicId,
			@RequestParam(required = false) LeaveRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		LeaveRequestFilter filter = new LeaveRequestFilter(null, leaveTypePublicId, status, new DateWindow(from, to));
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(
						leaveRequestService.listForCurrentEmployee(filter, pageableResolver.resolve(page, size)),
						leaveRequestMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.LEAVE_SELF_SERVICE)
	public ApiResponse<LeaveRequestResponse> submit(@Valid @RequestBody SubmitLeaveRequestRequest request) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.submit(request.leaveTypePublicId(),
				request.startDate(), request.endDate(), request.reason())));
	}

	@Override
	@GetMapping("/awaiting-review")
	@RequiresPermission(SchoolPermissions.LEAVE_REQUEST_REVIEW)
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listAwaitingMyReview(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(
						leaveRequestService.listAwaitingMyReview(pageableResolver.resolve(page, size)),
						leaveRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}/approvals")
	@RequiresPermission(SchoolPermissions.LEAVE_REQUEST_MANAGE)
	public ApiResponse<List<LeaveApprovalResponse>> listApprovals(@PathVariable String publicId) {
		return ApiResponse.success(referenceSummaries.mapList(leaveRequestService.listApprovals(publicId),
				leaveApprovalMapper::toResponse));
	}

	@Override
	@PatchMapping("/{publicId}/approve")
	@RequiresPermission({ SchoolPermissions.LEAVE_REQUEST_MANAGE, SchoolPermissions.LEAVE_REQUEST_REVIEW })
	public ApiResponse<LeaveRequestResponse> approve(@PathVariable String publicId) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.approve(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/reject")
	@Command
	@RequiresPermission({ SchoolPermissions.LEAVE_REQUEST_MANAGE, SchoolPermissions.LEAVE_REQUEST_REVIEW })
	public ApiResponse<LeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request) {
		return ApiResponse.success(
				leaveRequestMapper.toResponse(leaveRequestService.reject(publicId, request.rejectionReason())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@RequiresPermission(SchoolPermissions.LEAVE_SELF_SERVICE)
	public ApiResponse<LeaveRequestResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.cancel(publicId)));
	}
}

package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.altafjava.school.application.service.LeaveRequestService;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController implements LeaveRequestApi {

	// Either permission opens the endpoint; the service then checks the caller may decide at the level
	// this particular request is waiting on.
	private static final String DECIDE_LEAVE = "@permissionAuthorizationService.hasPermission('LEAVE_REQUEST_MANAGE') "
			+ "or @permissionAuthorizationService.hasPermission('LEAVE_REQUEST_REVIEW')";

	private final LeaveRequestService leaveRequestService;
	private final LeaveRequestMapper leaveRequestMapper;
	private final LeaveApprovalMapper leaveApprovalMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LeaveRequestController(LeaveRequestService leaveRequestService, LeaveRequestMapper leaveRequestMapper,
			LeaveApprovalMapper leaveApprovalMapper, SpringDataPageableResolver pageableResolver) {
		this.leaveRequestService = leaveRequestService;
		this.leaveRequestMapper = leaveRequestMapper;
		this.leaveApprovalMapper = leaveApprovalMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_REQUEST_MANAGE')")
	@SortableBy({ "startDate", "endDate", "status", "daysRequested" })
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(leaveRequestService.listAll(pageableResolver.resolve(page, size))
						.map(leaveRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/my")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_SELF_SERVICE')")
	@SortableBy({ "startDate", "endDate", "status", "daysRequested" })
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(leaveRequestService.listForCurrentEmployee(pageableResolver.resolve(page, size))
						.map(leaveRequestMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_SELF_SERVICE')")
	public ApiResponse<LeaveRequestResponse> submit(@Valid @RequestBody SubmitLeaveRequestRequest request) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.submit(request.leaveTypePublicId(),
				request.startDate(), request.endDate(), request.reason())));
	}

	@Override
	@GetMapping("/awaiting-review")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_REQUEST_REVIEW')")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listAwaitingMyReview(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(leaveRequestService.listAwaitingMyReview(pageableResolver.resolve(page, size))
						.map(leaveRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}/approvals")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_REQUEST_MANAGE')")
	public ApiResponse<List<LeaveApprovalResponse>> listApprovals(@PathVariable String publicId) {
		return ApiResponse.success(leaveRequestService.listApprovals(publicId).stream()
				.map(leaveApprovalMapper::toResponse).toList());
	}

	@Override
	@PatchMapping("/{publicId}/approve")
	@PreAuthorize(DECIDE_LEAVE)
	public ApiResponse<LeaveRequestResponse> approve(@PathVariable String publicId) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.approve(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/reject")
	@PreAuthorize(DECIDE_LEAVE)
	public ApiResponse<LeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request) {
		return ApiResponse.success(
				leaveRequestMapper.toResponse(leaveRequestService.reject(publicId, request.rejectionReason())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_SELF_SERVICE')")
	public ApiResponse<LeaveRequestResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(leaveRequestMapper.toResponse(leaveRequestService.cancel(publicId)));
	}
}

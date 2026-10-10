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
import com.altafjava.school.api.controller.api.LeaveTypeApi;
import com.altafjava.school.api.dto.request.ConfigureLeaveApprovalRequest;
import com.altafjava.school.api.dto.request.ConfigureLeaveCarryForwardRequest;
import com.altafjava.school.api.dto.request.CreateLeaveTypeRequest;
import com.altafjava.school.api.dto.request.UpdateLeaveTypeRequest;
import com.altafjava.school.api.dto.response.LeaveTypeResponse;
import com.altafjava.school.api.mapper.LeaveTypeMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.LeaveTypeService;

@RestController
@RequestMapping("/api/v1/leave-types")
public class LeaveTypeController implements LeaveTypeApi {

	private final LeaveTypeService leaveTypeService;
	private final LeaveTypeMapper leaveTypeMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LeaveTypeController(LeaveTypeService leaveTypeService, LeaveTypeMapper leaveTypeMapper,
			SpringDataPageableResolver pageableResolver) {
		this.leaveTypeService = leaveTypeService;
		this.leaveTypeMapper = leaveTypeMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveTypeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				leaveTypeService.list(q, pageableResolver.resolve(page, size)).map(leaveTypeMapper::toResponse)));
	}

	@Override
	@GetMapping("/active")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public ApiResponse<List<LeaveTypeResponse>> listActive() {
		return ApiResponse.success(leaveTypeService.listActive().stream().map(leaveTypeMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_READ')")
	public ApiResponse<LeaveTypeResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> create(@Valid @RequestBody CreateLeaveTypeRequest request) {
		return ApiResponse.success(
				leaveTypeMapper.toResponse(leaveTypeService.create(request.name(), request.defaultAnnualDays())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateLeaveTypeRequest request) {
		return ApiResponse.success(leaveTypeMapper
				.toResponse(leaveTypeService.updateDetails(publicId, request.name(), request.defaultAnnualDays(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.deactivate(publicId)));
	}

	// Drives PayrollCalculator's loss-of-pay basis (see PayslipService) — schools that want a leave
	// category excluded from pay mark it here rather than payroll inventing its own catalog.
	@Override
	@PatchMapping("/{publicId}/mark-unpaid")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> markUnpaid(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.markUnpaid(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/mark-paid")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> markPaid(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.markPaid(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/restrict-during-probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> restrictDuringProbation(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.restrictDuringProbation(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/allow-during-probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> allowDuringProbation(@PathVariable String publicId) {
		return ApiResponse.success(leaveTypeMapper.toResponse(leaveTypeService.allowDuringProbation(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/carry-forward")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> configureCarryForward(@PathVariable String publicId,
			@Valid @RequestBody ConfigureLeaveCarryForwardRequest request) {
		return ApiResponse
				.success(leaveTypeMapper.toResponse(leaveTypeService.configureCarryForward(publicId, request.enabled(),
						request.maxCarryForwardDays(), request.carryForwardExpiryMonths(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/approval-levels")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_TYPE_WRITE')")
	public ApiResponse<LeaveTypeResponse> configureApprovalLevels(@PathVariable String publicId,
			@Valid @RequestBody ConfigureLeaveApprovalRequest request) {
		return ApiResponse.success(leaveTypeMapper
				.toResponse(leaveTypeService.configureApprovalLevels(publicId, request.approvalLevels(),
						request.expectedVersion())));
	}
}

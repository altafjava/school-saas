package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.LeaveBalanceApi;
import com.altafjava.school.api.dto.response.LeaveBalanceResponse;
import com.altafjava.school.api.mapper.LeaveBalanceMapper;
import com.altafjava.school.application.service.LeaveBalanceService;

@RestController
public class LeaveBalanceController implements LeaveBalanceApi {

	private final LeaveBalanceService leaveBalanceService;
	private final LeaveBalanceMapper leaveBalanceMapper;

	public LeaveBalanceController(LeaveBalanceService leaveBalanceService, LeaveBalanceMapper leaveBalanceMapper) {
		this.leaveBalanceService = leaveBalanceService;
		this.leaveBalanceMapper = leaveBalanceMapper;
	}

	@Override
	@GetMapping("/api/v1/employees/{publicId}/leave-balances")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_BALANCE_MANAGE')")
	public ApiResponse<List<LeaveBalanceResponse>> forEmployee(@PathVariable String publicId,
			@RequestParam String academicYearPublicId) {
		return ApiResponse.success(leaveBalanceService.listForEmployee(publicId, academicYearPublicId).stream()
				.map(leaveBalanceMapper::toResponse)
				.toList());
	}

	@Override
	@GetMapping("/api/v1/leave-requests/my/balances")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LEAVE_SELF_SERVICE')")
	public ApiResponse<List<LeaveBalanceResponse>> forCurrentEmployee(@RequestParam String academicYearPublicId) {
		return ApiResponse.success(leaveBalanceService.listForCurrentEmployee(academicYearPublicId).stream()
				.map(leaveBalanceMapper::toResponse)
				.toList());
	}
}

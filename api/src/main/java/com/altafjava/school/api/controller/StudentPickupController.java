package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.StudentPickupApi;
import com.altafjava.school.api.dto.response.GuardianAuthorizationChangeResponse;
import com.altafjava.school.api.dto.response.PickupAuthorizedGuardianResponse;
import com.altafjava.school.api.dto.response.PickupCheckResponse;
import com.altafjava.school.api.mapper.GuardianAuthorizationChangeMapper;
import com.altafjava.school.api.mapper.PickupAuthorizedGuardianMapper;
import com.altafjava.school.application.service.GuardianPickupService;

@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/pickup-authorizations")
public class StudentPickupController implements StudentPickupApi {

	private final GuardianPickupService guardianPickupService;
	private final PickupAuthorizedGuardianMapper pickupAuthorizedGuardianMapper;
	private final GuardianAuthorizationChangeMapper guardianAuthorizationChangeMapper;

	public StudentPickupController(GuardianPickupService guardianPickupService,
			PickupAuthorizedGuardianMapper pickupAuthorizedGuardianMapper,
			GuardianAuthorizationChangeMapper guardianAuthorizationChangeMapper) {
		this.guardianPickupService = guardianPickupService;
		this.pickupAuthorizedGuardianMapper = pickupAuthorizedGuardianMapper;
		this.guardianAuthorizationChangeMapper = guardianAuthorizationChangeMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_READ')")
	public ApiResponse<List<PickupAuthorizedGuardianResponse>> listAuthorized(@PathVariable String studentPublicId) {
		return ApiResponse.success(guardianPickupService.listAuthorized(studentPublicId).stream()
				.map(pickupAuthorizedGuardianMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/check")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_READ')")
	public ApiResponse<PickupCheckResponse> check(@PathVariable String studentPublicId,
			@RequestParam String guardianPublicId) {
		var decision = guardianPickupService.check(studentPublicId, guardianPublicId);
		return ApiResponse.success(new PickupCheckResponse(
				decision == com.altafjava.school.domain.guardian.model.PickupDecision.AUTHORIZED, decision.name()));
	}

	@Override
	@GetMapping("/history")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_MANAGE')")
	public ApiResponse<List<GuardianAuthorizationChangeResponse>> history(@PathVariable String studentPublicId) {
		return ApiResponse.success(guardianPickupService.history(studentPublicId).stream()
				.map(guardianAuthorizationChangeMapper::toResponse).toList());
	}
}

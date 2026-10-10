package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.GuardianPickupApi;
import com.altafjava.school.api.dto.request.RestrictCustodyRequest;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.api.mapper.StudentGuardianLinkMapper;
import com.altafjava.school.application.service.GuardianPickupService;

@RestController
@RequestMapping("/api/v1/guardians/{guardianPublicId}/students/{studentPublicId}")
public class GuardianPickupController implements GuardianPickupApi {

	private final GuardianPickupService guardianPickupService;
	private final StudentGuardianLinkMapper studentGuardianLinkMapper;

	public GuardianPickupController(GuardianPickupService guardianPickupService,
			StudentGuardianLinkMapper studentGuardianLinkMapper) {
		this.guardianPickupService = guardianPickupService;
		this.studentGuardianLinkMapper = studentGuardianLinkMapper;
	}

	@Override
	@PatchMapping("/pickup-authorization/grant")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> authorizePickup(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentGuardianLinkMapper
				.toResponse(guardianPickupService.authorizePickup(guardianPublicId, studentPublicId, user.getId())));
	}

	@Override
	@PatchMapping("/pickup-authorization/revoke")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> revokePickup(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentGuardianLinkMapper
				.toResponse(guardianPickupService.revokePickup(guardianPublicId, studentPublicId, user.getId())));
	}

	@Override
	@PatchMapping("/custody-restriction/apply")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> restrictCustody(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @Valid @RequestBody RestrictCustodyRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentGuardianLinkMapper.toResponse(guardianPickupService
				.restrictCustody(guardianPublicId, studentPublicId, request.note(), user.getId())));
	}

	@Override
	@PatchMapping("/custody-restriction/lift")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_PICKUP_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> liftCustodyRestriction(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(studentGuardianLinkMapper.toResponse(
				guardianPickupService.liftCustodyRestriction(guardianPublicId, studentPublicId, user.getId())));
	}
}

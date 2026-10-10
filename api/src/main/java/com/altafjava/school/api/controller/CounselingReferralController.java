package com.altafjava.school.api.controller;

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
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.school.api.controller.api.CounselingReferralApi;
import com.altafjava.school.api.dto.request.ReferForCounselingRequest;
import com.altafjava.school.api.dto.request.ScheduleCounselingReferralRequest;
import com.altafjava.school.api.dto.response.CounselingReferralResponse;
import com.altafjava.school.api.mapper.CounselingReferralMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CounselingReferralService;

// See CounselingSessionController for the TENANT_ADMIN-only rationale (PHI-grade data, no
// dedicated counselor role in the seeded catalog).
@RestController
@RequestMapping("/api/v1/counseling-referrals")
public class CounselingReferralController implements CounselingReferralApi {

	private final CounselingReferralService counselingReferralService;
	private final CounselingReferralMapper counselingReferralMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CounselingReferralController(CounselingReferralService counselingReferralService,
			CounselingReferralMapper counselingReferralMapper, SpringDataPageableResolver pageableResolver) {
		this.counselingReferralService = counselingReferralService;
		this.counselingReferralMapper = counselingReferralMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingReferralResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(counselingReferralService.listAll(pageableResolver.resolve(page, size))
						.map(counselingReferralMapper::toResponse)));
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingReferralResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				counselingReferralService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(counselingReferralMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingReferralResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(counselingReferralMapper.toResponse(counselingReferralService.get(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingReferralResponse> refer(@Valid @RequestBody ReferForCounselingRequest request) {
		return ApiResponse.success(counselingReferralMapper
				.toResponse(counselingReferralService.refer(request.studentPublicId(), request.reason())));
	}

	@Override
	@PatchMapping("/{publicId}/schedule")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingReferralResponse> schedule(@PathVariable String publicId,
			@Valid @RequestBody ScheduleCounselingReferralRequest request) {
		return ApiResponse.success(counselingReferralMapper.toResponse(
				counselingReferralService.scheduleWithSession(publicId, request.counselingSessionPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/complete")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingReferralResponse> complete(@PathVariable String publicId) {
		return ApiResponse.success(counselingReferralMapper.toResponse(counselingReferralService.complete(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/decline")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingReferralResponse> decline(@PathVariable String publicId) {
		return ApiResponse.success(counselingReferralMapper.toResponse(counselingReferralService.decline(publicId)));
	}
}

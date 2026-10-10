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
import com.altafjava.school.api.controller.api.CounselingSessionApi;
import com.altafjava.school.api.dto.request.ScheduleCounselingSessionRequest;
import com.altafjava.school.api.dto.request.UpdateCounselingSessionNotesRequest;
import com.altafjava.school.api.dto.response.CounselingSessionResponse;
import com.altafjava.school.api.mapper.CounselingSessionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CounselingSessionService;

/**
 * Gated to {@code Roles.HAS_TENANT_ADMIN} only on every endpoint, including reads — the same
 * confidentiality posture as {@code HealthRecordController}/{@code MedicalIncidentController}, since
 * counseling notes are PHI-grade confidential data. No dedicated counselor/school-psychologist role
 * exists in the seeded role catalog; a dedicated role is a follow-up.
 */
@RestController
@RequestMapping("/api/v1/counseling-sessions")
public class CounselingSessionController implements CounselingSessionApi {

	private final CounselingSessionService counselingSessionService;
	private final CounselingSessionMapper counselingSessionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CounselingSessionController(CounselingSessionService counselingSessionService,
			CounselingSessionMapper counselingSessionMapper, SpringDataPageableResolver pageableResolver) {
		this.counselingSessionService = counselingSessionService;
		this.counselingSessionMapper = counselingSessionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(counselingSessionService.listAll(pageableResolver.resolve(page, size))
						.map(counselingSessionMapper::toResponse)));
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				counselingSessionService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(counselingSessionMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingSessionResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(counselingSessionMapper.toResponse(counselingSessionService.get(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingSessionResponse> schedule(
			@Valid @RequestBody ScheduleCounselingSessionRequest request) {
		return ApiResponse
				.success(counselingSessionMapper.toResponse(counselingSessionService.schedule(request.studentPublicId(),
						request.counselorTeacherPublicId(), request.sessionDate(), request.notes(),
						request.followUpRequired())));
	}

	@Override
	@PatchMapping("/{publicId}/notes")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('COUNSELING_MANAGE')")
	public ApiResponse<CounselingSessionResponse> updateNotes(@PathVariable String publicId,
			@Valid @RequestBody UpdateCounselingSessionNotesRequest request) {
		return ApiResponse.success(counselingSessionMapper.toResponse(
				counselingSessionService.updateNotes(publicId, request.notes(), request.followUpRequired(),
						request.expectedVersion())));
	}
}

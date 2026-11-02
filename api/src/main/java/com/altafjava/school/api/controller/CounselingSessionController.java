package com.altafjava.school.api.controller;

import java.time.LocalDate;
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
import com.altafjava.school.api.controller.api.CounselingSessionApi;
import com.altafjava.school.api.dto.request.ScheduleCounselingSessionRequest;
import com.altafjava.school.api.dto.request.UpdateCounselingSessionNotesRequest;
import com.altafjava.school.api.dto.response.CounselingSessionResponse;
import com.altafjava.school.api.mapper.CounselingSessionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.CounselingSessionFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.CounselingSessionService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

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

	private final ReferenceSummaries referenceSummaries;

	public CounselingSessionController(CounselingSessionService counselingSessionService,
			CounselingSessionMapper counselingSessionMapper, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.counselingSessionService = counselingSessionService;
		this.counselingSessionMapper = counselingSessionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.COUNSELING_MANAGE)
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) Boolean followUpRequired,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(counselingSessionService
						.listAll(new CounselingSessionFilter(studentPublicId, new DateWindow(from, to),
								followUpRequired), pageableResolver.resolve(page, size)),
						counselingSessionMapper::toResponse)));
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@RequiresPermission(SchoolPermissions.COUNSELING_MANAGE)
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				counselingSessionService.listForStudent(studentPublicId, pageableResolver.resolve(page, size)),
				counselingSessionMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.COUNSELING_MANAGE)
	public ApiResponse<CounselingSessionResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(counselingSessionMapper.toResponse(counselingSessionService.get(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.COUNSELING_MANAGE)
	public ApiResponse<CounselingSessionResponse> schedule(
			@Valid @RequestBody ScheduleCounselingSessionRequest request) {
		return ApiResponse
				.success(counselingSessionMapper.toResponse(counselingSessionService.schedule(request.studentPublicId(),
						request.counselorTeacherPublicId(), request.sessionDate(), request.notes(),
						request.followUpRequired())));
	}

	@Override
	@PatchMapping("/{publicId}/notes")
	@RequiresPermission(SchoolPermissions.COUNSELING_MANAGE)
	public ApiResponse<CounselingSessionResponse> updateNotes(@PathVariable String publicId,
			@Valid @RequestBody UpdateCounselingSessionNotesRequest request) {
		return ApiResponse.success(counselingSessionMapper.toResponse(
				counselingSessionService.updateNotes(publicId, request.notes(), request.followUpRequired(),
						request.expectedVersion())));
	}
}

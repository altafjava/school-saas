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
import com.altafjava.school.api.controller.api.SubjectApi;
import com.altafjava.school.api.dto.request.AssignSubjectCurriculumRequest;
import com.altafjava.school.api.dto.request.CreateSubjectRequest;
import com.altafjava.school.api.dto.response.SubjectResponse;
import com.altafjava.school.api.mapper.SubjectMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.SubjectService;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController implements SubjectApi {

	private final SubjectService subjectService;
	private final SubjectMapper subjectMapper;

	private final SpringDataPageableResolver pageableResolver;

	public SubjectController(SubjectService subjectService, SubjectMapper subjectMapper,
			SpringDataPageableResolver pageableResolver) {
		this.subjectService = subjectService;
		this.subjectMapper = subjectMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBJECT_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<SubjectResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(subjectService.searchSubjects(pageableResolver.resolve(page, size), q)
						.map(subjectMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBJECT_READ')")
	public ApiResponse<SubjectResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(subjectMapper.toResponse(subjectService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBJECT_WRITE')")
	public ApiResponse<SubjectResponse> create(@Valid @RequestBody CreateSubjectRequest request) {
		return ApiResponse.success(subjectMapper.toResponse(subjectService.create(
				request.code(),
				request.name(),
				request.description())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBJECT_WRITE')")
	public ApiResponse<SubjectResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(subjectMapper.toResponse(subjectService.deactivate(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/curriculum")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBJECT_WRITE')")
	public ApiResponse<SubjectResponse> assignCurriculum(@PathVariable String publicId,
			@Valid @RequestBody AssignSubjectCurriculumRequest request) {
		return ApiResponse.success(
				subjectMapper.toResponse(subjectService.assignCurriculum(publicId, request.curriculumPublicId(),
						request.expectedVersion())));
	}
}

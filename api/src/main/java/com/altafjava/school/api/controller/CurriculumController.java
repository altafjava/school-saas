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
import com.altafjava.school.api.controller.api.CurriculumApi;
import com.altafjava.school.api.dto.request.AssignGradingScaleRequest;
import com.altafjava.school.api.dto.request.CreateCurriculumRequest;
import com.altafjava.school.api.dto.request.UpdateCurriculumRequest;
import com.altafjava.school.api.dto.response.CurriculumResponse;
import com.altafjava.school.api.mapper.CurriculumMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CurriculumService;

@RestController
@RequestMapping("/api/v1/curricula")
public class CurriculumController implements CurriculumApi {

	private final CurriculumService curriculumService;
	private final CurriculumMapper curriculumMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CurriculumController(CurriculumService curriculumService, CurriculumMapper curriculumMapper,
			SpringDataPageableResolver pageableResolver) {
		this.curriculumService = curriculumService;
		this.curriculumMapper = curriculumMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<CurriculumResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				curriculumService.list(q, pageableResolver.resolve(page, size)).map(curriculumMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_READ')")
	public ApiResponse<CurriculumResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(curriculumMapper.toResponse(curriculumService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_WRITE')")
	public ApiResponse<CurriculumResponse> create(@Valid @RequestBody CreateCurriculumRequest request) {
		return ApiResponse
				.success(curriculumMapper.toResponse(curriculumService.create(request.boardPublicId(), request.name(),
						request.code(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_WRITE')")
	public ApiResponse<CurriculumResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCurriculumRequest request) {
		return ApiResponse.success(curriculumMapper.toResponse(
				curriculumService.updateDetails(publicId, request.name(), request.code(), request.description(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/grading-scale")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_WRITE')")
	public ApiResponse<CurriculumResponse> assignGradingScale(@PathVariable String publicId,
			@Valid @RequestBody AssignGradingScaleRequest request) {
		return ApiResponse.success(curriculumMapper
				.toResponse(curriculumService.assignGradingScale(publicId, request.gradingScalePublicId(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CURRICULUM_WRITE')")
	public ApiResponse<CurriculumResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(curriculumMapper.toResponse(curriculumService.deactivate(publicId)));
	}
}

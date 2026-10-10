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
import com.altafjava.school.api.controller.api.SubmissionApi;
import com.altafjava.school.api.dto.request.GradeSubmissionRequest;
import com.altafjava.school.api.dto.request.SubmitAssignmentRequest;
import com.altafjava.school.api.dto.response.SubmissionResponse;
import com.altafjava.school.api.mapper.SubmissionMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.SubmissionService;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentPublicId}/submissions")
public class SubmissionController implements SubmissionApi {

	private final SubmissionService submissionService;
	private final SubmissionMapper submissionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public SubmissionController(SubmissionService submissionService, SubmissionMapper submissionMapper,
			SpringDataPageableResolver pageableResolver) {
		this.submissionService = submissionService;
		this.submissionMapper = submissionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBMISSION_SUBMIT')")
	public ApiResponse<SubmissionResponse> submit(@PathVariable String assignmentPublicId,
			@Valid @RequestBody SubmitAssignmentRequest request) {
		return ApiResponse.success(submissionMapper.toResponse(
				submissionService.submit(assignmentPublicId, request.storageKey(), request.textContent())));
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBMISSION_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<SubmissionResponse>> list(
			@PathVariable String assignmentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(submissionService.list(assignmentPublicId, pageableResolver.resolve(page, size))
						.map(submissionMapper::toResponse)));
	}

	@Override
	@PatchMapping("/{submissionPublicId}/grade")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBMISSION_GRADE')")
	public ApiResponse<SubmissionResponse> grade(@PathVariable String assignmentPublicId,
			@PathVariable String submissionPublicId, @Valid @RequestBody GradeSubmissionRequest request) {
		return ApiResponse
				.success(submissionMapper.toResponse(submissionService.grade(assignmentPublicId, submissionPublicId,
						request.marks(), request.feedback())));
	}
}

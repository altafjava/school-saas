package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.ExamApi;
import com.altafjava.school.api.dto.request.AssignExamTermRequest;
import com.altafjava.school.api.dto.request.RescheduleExamRequest;
import com.altafjava.school.api.dto.request.ReweightExamRequest;
import com.altafjava.school.api.dto.request.ScheduleExamRequest;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.api.mapper.ExamMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.ExamService;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController implements ExamApi {

	private final ExamService examService;
	private final ExamMapper examMapper;

	private final SpringDataPageableResolver pageableResolver;

	public ExamController(ExamService examService, ExamMapper examMapper, SpringDataPageableResolver pageableResolver) {
		this.examService = examService;
		this.examMapper = examMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	@SortableBy({ "title", "scheduledAt", "status", "maxMarks" })
	public ApiResponse<com.altafjava.platform.core.model.Page<ExamResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse
				.success(PlatformPageMapper.toPlatformPage(examService.listExams(pageableResolver.resolve(page, size))
						.map(examMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	public ApiResponse<ExamResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ApiResponse<ExamResponse> schedule(@Valid @RequestBody ScheduleExamRequest request) {
		return ApiResponse.success(examMapper.toResponse(examService.schedule(
				request.title(),
				request.subjectId(),
				request.classroomId(),
				request.scheduledAt(),
				request.maxMarks(),
				request.termId(),
				request.examTypeId(),
				request.weightage())));
	}

	@Override
	@PatchMapping("/{publicId}/schedule")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ApiResponse<ExamResponse> reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleExamRequest request) {
		return ApiResponse.success(examMapper.toResponse(examService.reschedule(publicId, request.scheduledAt())));
	}

	@Override
	@PatchMapping("/{publicId}/term")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ApiResponse<ExamResponse> assignTerm(@PathVariable String publicId,
			@Valid @RequestBody AssignExamTermRequest request) {
		return ApiResponse.success(examMapper.toResponse(examService.assignTerm(publicId, request.termId())));
	}

	@Override
	@PatchMapping("/{publicId}/weightage")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ApiResponse<ExamResponse> reweight(@PathVariable String publicId,
			@Valid @RequestBody ReweightExamRequest request) {
		return ApiResponse.success(examMapper.toResponse(examService.reweight(publicId, request.weightage())));
	}

	@Override
	@PostMapping("/{publicId}/results/publish")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_RESULT_PUBLISH')")
	public ApiResponse<ExamResponse> publishResults(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(examMapper.toResponse(examService.publishResults(publicId, user.getUsername())));
	}

	@Override
	@PostMapping("/{publicId}/results/withdraw")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_RESULT_PUBLISH')")
	public ApiResponse<ExamResponse> withdrawResults(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.withdrawResults(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/complete")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_COMPLETE')")
	public ApiResponse<ExamResponse> complete(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.complete(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EXAM_WRITE')")
	public ApiResponse<ExamResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.cancel(publicId)));
	}
}

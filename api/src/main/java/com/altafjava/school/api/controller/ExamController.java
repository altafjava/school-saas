package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import com.altafjava.platform.application.security.annotation.RequiresPermission;
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
import com.altafjava.school.application.exam.ExamMarksService;
import com.altafjava.school.application.filter.ExamFilter;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.ExamService;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController implements ExamApi {

	private final ExamService examService;
	private final ExamMarksService examMarksService;
	private final ExamMapper examMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public ExamController(ExamService examService, ExamMarksService examMarksService, ExamMapper examMapper,
			SpringDataPageableResolver pageableResolver, ReferenceSummaries referenceSummaries) {
		this.examMarksService = examMarksService;
		this.referenceSummaries = referenceSummaries;
		this.examService = examService;
		this.examMapper = examMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.EXAM_READ)
	@SortableBy({ "title", "scheduledAt", "status", "maxMarks" })
	public ApiResponse<com.altafjava.platform.core.model.Page<ExamResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String termPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) ExamStatus status) {
		ExamFilter filter = new ExamFilter(classroomPublicId, termPublicId, subjectPublicId, status);
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(
						examService.listExams(filter, pageableResolver.resolve(page, size)), examMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.EXAM_READ)
	public ApiResponse<ExamResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	public ApiResponse<ExamResponse> schedule(@Valid @RequestBody ScheduleExamRequest request) {
		return ApiResponse.success(examMapper.toResponse(examService.schedule(
				new ExamService.ExamDetails(request.title(), request.subjectPublicId(), request.classroomPublicId(),
						request.maxMarks(), request.termPublicId(), request.examTypePublicId(), request.weightage()),
				new ExamService.SlotRequest(request.scheduledAt(), request.durationMinutes(),
						request.venuePublicId()))));
	}

	@Override
	@PatchMapping("/{publicId}/schedule")
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	public ApiResponse<ExamResponse> reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleExamRequest request) {
		return ApiResponse.success(examMapper
				.toResponse(examService.reschedule(publicId, new ExamService.SlotRequest(request.scheduledAt(),
						request.durationMinutes(), request.venuePublicId()), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/term")
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	public ApiResponse<ExamResponse> assignTerm(@PathVariable String publicId,
			@Valid @RequestBody AssignExamTermRequest request) {
		return ApiResponse.success(examMapper
				.toResponse(examService.assignTerm(publicId, request.termPublicId(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/weightage")
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	public ApiResponse<ExamResponse> reweight(@PathVariable String publicId,
			@Valid @RequestBody ReweightExamRequest request) {
		return ApiResponse.success(
				examMapper.toResponse(examService.reweight(publicId, request.weightage(), request.expectedVersion())));
	}

	@Override
	@PostMapping("/{publicId}/results/publish")
	@RequiresPermission(SchoolPermissions.EXAM_RESULT_PUBLISH)
	public ApiResponse<ExamResponse> publishResults(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(examMapper.toResponse(examService.publishResults(publicId, user.getUsername())));
	}

	@Override
	@PostMapping("/{publicId}/results/withdraw")
	@RequiresPermission(SchoolPermissions.EXAM_RESULT_PUBLISH)
	public ApiResponse<ExamResponse> withdrawResults(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.withdrawResults(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/complete")
	@RequiresPermission(SchoolPermissions.EXAM_COMPLETE)
	public ApiResponse<ExamResponse> complete(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.complete(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	public ApiResponse<ExamResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examService.cancel(publicId)));
	}

	@Override
	@PostMapping("/{publicId}/marks/verify")
	@RequiresPermission(SchoolPermissions.EXAM_MARKS_VERIFY)
	public ApiResponse<ExamResponse> verifyMarks(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(examMapper.toResponse(examMarksService.verify(publicId, user.getUsername())));
	}

	@Override
	@PostMapping("/{publicId}/marks/return")
	@RequiresPermission(SchoolPermissions.EXAM_MARKS_VERIFY)
	public ApiResponse<ExamResponse> returnMarks(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examMarksService.returnForCorrection(publicId)));
	}

	@Override
	@PostMapping("/{publicId}/marks/lock")
	@RequiresPermission(SchoolPermissions.EXAM_RESULT_PUBLISH)
	public ApiResponse<ExamResponse> lockMarks(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(examMapper.toResponse(examMarksService.lock(publicId, user.getUsername())));
	}

	@Override
	@PostMapping("/{publicId}/marks/unlock")
	@RequiresPermission(SchoolPermissions.EXAM_RESULT_PUBLISH)
	public ApiResponse<ExamResponse> unlockMarks(@PathVariable String publicId) {
		return ApiResponse.success(examMapper.toResponse(examMarksService.unlock(publicId)));
	}
}

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
import com.altafjava.school.api.controller.api.GradeApi;
import com.altafjava.school.api.dto.request.CorrectGradeRequest;
import com.altafjava.school.api.dto.request.RecordGradeRequest;
import com.altafjava.school.api.dto.response.GradeCorrectionResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.mapper.GradeCorrectionMapper;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.GradeService;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController implements GradeApi {

	private final GradeService gradeService;
	private final GradeMapper gradeMapper;
	private final GradeCorrectionMapper gradeCorrectionMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GradeController(GradeService gradeService, GradeMapper gradeMapper,
			GradeCorrectionMapper gradeCorrectionMapper, SpringDataPageableResolver pageableResolver) {
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
		this.gradeCorrectionMapper = gradeCorrectionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_READ')")
	@SortableBy({ "marks", "gradeLetter" })
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse
				.success(PlatformPageMapper.toPlatformPage(gradeService.listGrades(pageableResolver.resolve(page, size))
						.map(gradeMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_READ')")
	public ApiResponse<GradeResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(gradeMapper.toResponse(gradeService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_WRITE')")
	public ApiResponse<GradeResponse> record(@Valid @RequestBody RecordGradeRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(gradeMapper.toResponse(gradeService.record(
				request.studentPublicId(),
				request.examPublicId(),
				request.marks(),
				user.getUsername())));
	}

	@Override
	@PatchMapping("/{publicId}/marks")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_WRITE')")
	public ApiResponse<GradeResponse> correct(@PathVariable String publicId,
			@Valid @RequestBody CorrectGradeRequest request) {
		return ApiResponse.success(
				gradeMapper.toResponse(gradeService.correct(publicId, request.marks(), request.expectedVersion())));
	}

	@Override
	@GetMapping("/{publicId}/corrections")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_GRADES_WRITE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeCorrectionResponse>> listCorrections(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(gradeService.listCorrections(publicId, pageableResolver.resolve(page, size))
						.map(gradeCorrectionMapper::toResponse)));
	}
}

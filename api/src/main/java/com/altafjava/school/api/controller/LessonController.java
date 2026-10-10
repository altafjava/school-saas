package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.LessonApi;
import com.altafjava.school.api.dto.request.PostLessonRequest;
import com.altafjava.school.api.dto.response.LessonResponse;
import com.altafjava.school.api.mapper.LessonMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.CourseworkFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.service.LessonService;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController implements LessonApi {

	private final LessonService lessonService;
	private final LessonMapper lessonMapper;

	private final SpringDataPageableResolver pageableResolver;

	public LessonController(LessonService lessonService, LessonMapper lessonMapper,
			SpringDataPageableResolver pageableResolver) {
		this.lessonService = lessonService;
		this.lessonMapper = lessonMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LESSON_WRITE')")
	public ApiResponse<LessonResponse> post(@Valid @RequestBody PostLessonRequest request) {
		return ApiResponse.success(lessonMapper.toResponse(lessonService.post(
				request.classroomPublicId(),
				request.subjectPublicId(),
				request.title(),
				request.description(),
				request.storageKey())));
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('LESSON_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<LessonResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		CourseworkFilter filter = new CourseworkFilter(classroomPublicId, subjectPublicId, new DateWindow(from, to));
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(lessonService.listLessons(filter, pageableResolver.resolve(page, size))
						.map(lessonMapper::toResponse)));
	}
}

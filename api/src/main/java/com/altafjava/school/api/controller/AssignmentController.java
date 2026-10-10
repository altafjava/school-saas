package com.altafjava.school.api.controller;

import java.time.LocalDate;
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
import com.altafjava.school.api.controller.api.AssignmentApi;
import com.altafjava.school.api.dto.request.CreateAssignmentRequest;
import com.altafjava.school.api.dto.request.RescheduleAssignmentRequest;
import com.altafjava.school.api.dto.response.AssignmentResponse;
import com.altafjava.school.api.mapper.AssignmentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.CourseworkFilter;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.service.AssignmentService;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController implements AssignmentApi {

	private final AssignmentService assignmentService;
	private final AssignmentMapper assignmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AssignmentController(AssignmentService assignmentService, AssignmentMapper assignmentMapper,
			SpringDataPageableResolver pageableResolver) {
		this.assignmentService = assignmentService;
		this.assignmentMapper = assignmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSIGNMENT_WRITE')")
	public ApiResponse<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
		return ApiResponse.success(assignmentMapper.toResponse(assignmentService.create(
				request.classroomPublicId(),
				request.subjectPublicId(),
				request.title(),
				request.description(),
				request.storageKey(),
				request.dueDate(),
				request.maxMarks())));
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSIGNMENT_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssignmentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		CourseworkFilter filter = new CourseworkFilter(classroomPublicId, subjectPublicId, new DateWindow(from, to));
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(assignmentService.listAssignments(filter, pageableResolver.resolve(page, size))
						.map(assignmentMapper::toResponse)));
	}

	@Override
	@PatchMapping("/{publicId}/reschedule")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ASSIGNMENT_WRITE')")
	public ApiResponse<AssignmentResponse> reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleAssignmentRequest request) {
		return ApiResponse
				.success(assignmentMapper.toResponse(
						assignmentService.reschedule(publicId, request.dueDate(), request.expectedVersion())));
	}
}

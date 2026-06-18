package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.school.api.dto.request.CreateClassroomRequest;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.api.mapper.ClassroomMapper;
import com.altafjava.school.application.service.ClassroomService;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {

	private final ClassroomService classroomService;
	private final ClassroomMapper classroomMapper;

	public ClassroomController(ClassroomService classroomService, ClassroomMapper classroomMapper) {
		this.classroomService = classroomService;
		this.classroomMapper = classroomMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<ClassroomResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return classroomService.listClassrooms(PageRequest.of(page, Math.min(size, 100)))
				.map(classroomMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public ClassroomResponse get(@PathVariable String publicId) {
		return classroomMapper.toResponse(classroomService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public ClassroomResponse create(@Valid @RequestBody CreateClassroomRequest request) {
		return classroomMapper.toResponse(classroomService.create(
				request.classCode(),
				request.grade(),
				request.section(),
				request.academicYear(),
				request.classTeacherId()));
	}
}

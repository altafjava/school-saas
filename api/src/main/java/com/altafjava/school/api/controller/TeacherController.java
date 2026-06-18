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
import com.altafjava.school.api.dto.request.CreateTeacherRequest;
import com.altafjava.school.api.dto.response.TeacherResponse;
import com.altafjava.school.api.mapper.TeacherMapper;
import com.altafjava.school.application.service.TeacherService;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController {

	private final TeacherService teacherService;
	private final TeacherMapper teacherMapper;

	public TeacherController(TeacherService teacherService, TeacherMapper teacherMapper) {
		this.teacherService = teacherService;
		this.teacherMapper = teacherMapper;
	}

	@GetMapping
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public Page<TeacherResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return teacherService.listTeachers(PageRequest.of(page, Math.min(size, 100)))
				.map(teacherMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public TeacherResponse get(@PathVariable String publicId) {
		return teacherMapper.toResponse(teacherService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public TeacherResponse hire(@Valid @RequestBody CreateTeacherRequest request) {
		return teacherMapper.toResponse(teacherService.hire(
				request.employeeCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.joinDate()));
	}
}

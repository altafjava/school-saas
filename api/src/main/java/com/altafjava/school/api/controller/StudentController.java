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
import com.altafjava.school.api.dto.request.CreateStudentRequest;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.domain.student.model.Student;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

	private final StudentService studentService;
	private final StudentMapper studentMapper;

	public StudentController(StudentService studentService, StudentMapper studentMapper) {
		this.studentService = studentService;
		this.studentMapper = studentMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<StudentResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return studentService.listStudents(PageRequest.of(page, Math.min(size, 100)))
				.map(studentMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public StudentResponse get(@PathVariable String publicId) {
		return studentMapper.toResponse(studentService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public StudentResponse enroll(@Valid @RequestBody CreateStudentRequest request) {
		Student student = studentService.enroll(
				request.studentCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.dateOfBirth());
		return studentMapper.toResponse(student);
	}
}

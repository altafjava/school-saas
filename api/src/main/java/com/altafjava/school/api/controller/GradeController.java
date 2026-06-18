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
import com.altafjava.school.api.dto.request.RecordGradeRequest;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.application.service.GradeService;

@RestController
@RequestMapping("/api/v1/grades")
public class GradeController {

	private final GradeService gradeService;
	private final GradeMapper gradeMapper;

	public GradeController(GradeService gradeService, GradeMapper gradeMapper) {
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<GradeResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return gradeService.listGrades(PageRequest.of(page, Math.min(size, 100)))
				.map(gradeMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public GradeResponse get(@PathVariable String publicId) {
		return gradeMapper.toResponse(gradeService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public GradeResponse record(@Valid @RequestBody RecordGradeRequest request) {
		return gradeMapper.toResponse(gradeService.record(
				request.studentId(),
				request.subject(),
				request.examId(),
				request.marks(),
				request.gradeLetter(),
				request.gradedBy()));
	}
}

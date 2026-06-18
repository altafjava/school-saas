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
import com.altafjava.school.api.dto.request.ScheduleExamRequest;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.api.mapper.ExamMapper;
import com.altafjava.school.application.service.ExamService;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

	private final ExamService examService;
	private final ExamMapper examMapper;

	public ExamController(ExamService examService, ExamMapper examMapper) {
		this.examService = examService;
		this.examMapper = examMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<ExamResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return examService.listExams(PageRequest.of(page, Math.min(size, 100)))
				.map(examMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public ExamResponse get(@PathVariable String publicId) {
		return examMapper.toResponse(examService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public ExamResponse schedule(@Valid @RequestBody ScheduleExamRequest request) {
		return examMapper.toResponse(examService.schedule(
				request.title(),
				request.subject(),
				request.classroomId(),
				request.scheduledAt(),
				request.maxMarks()));
	}
}

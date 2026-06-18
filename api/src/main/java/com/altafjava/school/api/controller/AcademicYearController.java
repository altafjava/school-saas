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
import com.altafjava.school.api.dto.request.CreateAcademicYearRequest;
import com.altafjava.school.api.dto.response.AcademicYearResponse;
import com.altafjava.school.api.mapper.AcademicYearMapper;
import com.altafjava.school.application.service.AcademicYearService;

@RestController
@RequestMapping("/api/v1/academic-years")
public class AcademicYearController {

	private final AcademicYearService academicYearService;
	private final AcademicYearMapper academicYearMapper;

	public AcademicYearController(AcademicYearService academicYearService, AcademicYearMapper academicYearMapper) {
		this.academicYearService = academicYearService;
		this.academicYearMapper = academicYearMapper;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public Page<AcademicYearResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return academicYearService.listAcademicYears(PageRequest.of(page, Math.min(size, 100)))
				.map(academicYearMapper::toResponse);
	}

	@GetMapping("/{publicId}")
	@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TEACHER')")
	public AcademicYearResponse get(@PathVariable String publicId) {
		return academicYearMapper.toResponse(academicYearService.findByPublicId(publicId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('TENANT_ADMIN')")
	public AcademicYearResponse create(@Valid @RequestBody CreateAcademicYearRequest request) {
		return academicYearMapper.toResponse(academicYearService.create(
				request.name(),
				request.startDate(),
				request.endDate(),
				request.current()));
	}
}

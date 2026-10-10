package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
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
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.AcademicYearApi;
import com.altafjava.school.api.dto.request.CreateAcademicYearRequest;
import com.altafjava.school.api.dto.response.AcademicYearResponse;
import com.altafjava.school.api.mapper.AcademicYearMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.AcademicYearService;

@RestController
@RequestMapping("/api/v1/academic-years")
public class AcademicYearController implements AcademicYearApi {

	private final AcademicYearService academicYearService;
	private final AcademicYearMapper academicYearMapper;

	private final SpringDataPageableResolver pageableResolver;

	public AcademicYearController(AcademicYearService academicYearService, AcademicYearMapper academicYearMapper,
			SpringDataPageableResolver pageableResolver) {
		this.academicYearService = academicYearService;
		this.academicYearMapper = academicYearMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ACADEMIC_YEAR_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AcademicYearResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(academicYearService.listAcademicYears(q, pageableResolver.resolve(page, size))
						.map(academicYearMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ACADEMIC_YEAR_READ')")
	public ApiResponse<AcademicYearResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(academicYearMapper.toResponse(academicYearService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ACADEMIC_YEAR_WRITE')")
	public ApiResponse<AcademicYearResponse> create(@Valid @RequestBody CreateAcademicYearRequest request) {
		return ApiResponse.success(academicYearMapper.toResponse(academicYearService.create(
				request.name(),
				request.startDate(),
				request.endDate(),
				request.current())));
	}
}

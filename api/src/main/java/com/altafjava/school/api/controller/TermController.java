package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.school.api.controller.api.TermApi;
import com.altafjava.school.api.dto.request.CreateTermRequest;
import com.altafjava.school.api.dto.response.TermResponse;
import com.altafjava.school.api.mapper.TermMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.TermFilter;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/terms")
public class TermController implements TermApi {

	private final TermService termService;
	private final TermMapper termMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public TermController(TermService termService, TermMapper termMapper, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.termService = termService;
		this.termMapper = termMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.TERM_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<TermResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String academicYearPublicId,
			@RequestParam(required = false) String q) {
		return ApiResponse
				.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(termService
						.listTerms(new TermFilter(academicYearPublicId, q), pageableResolver.resolve(page, size)),
						termMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.TERM_READ)
	public ApiResponse<TermResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(termMapper.toResponse(termService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.TERM_WRITE)
	public ApiResponse<TermResponse> create(@Valid @RequestBody CreateTermRequest request) {
		return ApiResponse.success(termMapper.toResponse(termService.create(
				request.name(),
				request.startDate(),
				request.endDate(),
				request.academicYearPublicId())));
	}
}

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
import com.altafjava.school.api.controller.api.PeriodApi;
import com.altafjava.school.api.dto.request.CreatePeriodRequest;
import com.altafjava.school.api.dto.response.PeriodResponse;
import com.altafjava.school.api.mapper.PeriodMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.PeriodService;

@RestController
@RequestMapping("/api/v1/periods")
public class PeriodController implements PeriodApi {

	private final PeriodService periodService;
	private final PeriodMapper periodMapper;

	private final SpringDataPageableResolver pageableResolver;

	public PeriodController(PeriodService periodService, PeriodMapper periodMapper,
			SpringDataPageableResolver pageableResolver) {
		this.periodService = periodService;
		this.periodMapper = periodMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<PeriodResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(periodService.listPeriods(q, pageableResolver.resolve(page, size))
						.map(periodMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_READ')")
	public ApiResponse<PeriodResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(periodMapper.toResponse(periodService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PERIOD_WRITE')")
	public ApiResponse<PeriodResponse> create(@Valid @RequestBody CreatePeriodRequest request) {
		return ApiResponse.success(periodMapper.toResponse(periodService.create(
				request.name(),
				request.startTime(),
				request.endTime(),
				request.displayOrder())));
	}
}

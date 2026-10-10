package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.altafjava.school.api.controller.api.HolidayApi;
import com.altafjava.school.api.dto.request.CreateHolidayRequest;
import com.altafjava.school.api.dto.request.UpdateHolidayRequest;
import com.altafjava.school.api.dto.response.HolidayResponse;
import com.altafjava.school.api.mapper.HolidayMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.HolidayService;

// The tenant's school-calendar holiday list — feeds attendance-percentage and leave-day
// calculations (see HolidayService#datesInRange), so read access is broad but writes are
// tenant-admin-only.
@RestController
@RequestMapping("/api/v1/holidays")
public class HolidayController implements HolidayApi {

	private final HolidayService holidayService;
	private final HolidayMapper holidayMapper;

	private final SpringDataPageableResolver pageableResolver;

	public HolidayController(HolidayService holidayService, HolidayMapper holidayMapper,
			SpringDataPageableResolver pageableResolver) {
		this.holidayService = holidayService;
		this.holidayMapper = holidayMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOLIDAY_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<HolidayResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				holidayService.list(pageableResolver.resolve(page, size)).map(holidayMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOLIDAY_READ')")
	public ApiResponse<HolidayResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(holidayMapper.toResponse(holidayService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOLIDAY_WRITE')")
	public ApiResponse<HolidayResponse> create(@Valid @RequestBody CreateHolidayRequest request) {
		return ApiResponse.success(
				holidayMapper.toResponse(holidayService.create(request.date(), request.name(), request.recurring())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOLIDAY_WRITE')")
	public ApiResponse<HolidayResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateHolidayRequest request) {
		return ApiResponse.success(holidayMapper.toResponse(
				holidayService.updateDetails(publicId, request.date(), request.name(), request.recurring(),
						request.expectedVersion())));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HOLIDAY_WRITE')")
	public ApiResponse<Void> delete(@PathVariable String publicId) {
		holidayService.delete(publicId);
		return ApiResponse.success(null);
	}
}

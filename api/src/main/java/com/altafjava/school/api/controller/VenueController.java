package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.altafjava.school.api.controller.api.VenueApi;
import com.altafjava.school.api.dto.request.CreateVenueRequest;
import com.altafjava.school.api.dto.request.UpdateVenueRequest;
import com.altafjava.school.api.dto.response.VenueResponse;
import com.altafjava.school.api.mapper.VenueMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.VenueService;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController implements VenueApi {

	private final VenueService venueService;
	private final VenueMapper venueMapper;
	private final SpringDataPageableResolver pageableResolver;

	public VenueController(VenueService venueService, VenueMapper venueMapper,
			SpringDataPageableResolver pageableResolver) {
		this.venueService = venueService;
		this.venueMapper = venueMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<VenueResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(venueService.list(pageableResolver.resolve(page, size)).map(venueMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_READ')")
	public ApiResponse<VenueResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(venueMapper.toResponse(venueService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VENUE_MANAGE')")
	public ApiResponse<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request) {
		return ApiResponse.success(venueMapper
				.toResponse(venueService.create(request.code(), request.name(), request.venueType(),
						request.capacity())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VENUE_MANAGE')")
	public ApiResponse<VenueResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdateVenueRequest request) {
		return ApiResponse.success(venueMapper.toResponse(
				venueService.update(publicId, request.name(), request.venueType(), request.capacity())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VENUE_MANAGE')")
	public ApiResponse<VenueResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(venueMapper.toResponse(venueService.deactivate(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/activate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VENUE_MANAGE')")
	public ApiResponse<VenueResponse> activate(@PathVariable String publicId) {
		return ApiResponse.success(venueMapper.toResponse(venueService.activate(publicId)));
	}
}

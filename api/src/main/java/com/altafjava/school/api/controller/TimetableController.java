package com.altafjava.school.api.controller;

import java.time.DayOfWeek;
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
import com.altafjava.school.api.controller.api.TimetableApi;
import com.altafjava.school.api.dto.request.AssignTimetableVenueRequest;
import com.altafjava.school.api.dto.request.CreateTimetableEntryRequest;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.TimetableEntryFilter;
import com.altafjava.school.application.service.TimetableService;

@RestController
@RequestMapping("/api/v1/timetable-entries")
public class TimetableController implements TimetableApi {

	private final TimetableService timetableService;
	private final TimetableEntryMapper timetableEntryMapper;

	private final SpringDataPageableResolver pageableResolver;

	public TimetableController(TimetableService timetableService, TimetableEntryMapper timetableEntryMapper,
			SpringDataPageableResolver pageableResolver) {
		this.timetableService = timetableService;
		this.timetableEntryMapper = timetableEntryMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<TimetableEntryResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String teacherPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) DayOfWeek dayOfWeek) {
		return ApiResponse.success(
				PlatformPageMapper
						.toPlatformPage(
								timetableService
										.listEntries(
												new TimetableEntryFilter(classroomPublicId, teacherPublicId,
														subjectPublicId, dayOfWeek),
												pageableResolver.resolve(page, size))
										.map(timetableEntryMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_READ')")
	public ApiResponse<TimetableEntryResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(timetableEntryMapper.toResponse(timetableService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_WRITE')")
	public ApiResponse<TimetableEntryResponse> schedule(@Valid @RequestBody CreateTimetableEntryRequest request) {
		return ApiResponse.success(timetableEntryMapper.toResponse(timetableService.schedule(
				request.dayOfWeek(),
				request.periodPublicId(),
				request.classroomPublicId(),
				request.subjectPublicId(),
				request.teacherPublicId(),
				request.venuePublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/venue")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_WRITE')")
	public ApiResponse<TimetableEntryResponse> assignVenue(@PathVariable String publicId,
			@Valid @RequestBody AssignTimetableVenueRequest request) {
		return ApiResponse.success(
				timetableEntryMapper.toResponse(
						timetableService.assignVenue(publicId, request.venuePublicId(), request.expectedVersion())));
	}
}

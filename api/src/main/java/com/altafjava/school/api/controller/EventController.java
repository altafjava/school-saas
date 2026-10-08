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
import com.altafjava.school.api.controller.api.EventApi;
import com.altafjava.school.api.dto.request.CreateEventRequest;
import com.altafjava.school.api.dto.request.RegisterForEventRequest;
import com.altafjava.school.api.dto.request.UpdateEventRequest;
import com.altafjava.school.api.dto.response.EventRegistrationResponse;
import com.altafjava.school.api.dto.response.EventResponse;
import com.altafjava.school.api.mapper.EventMapper;
import com.altafjava.school.api.mapper.EventRegistrationMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.EventRegistrationService;
import com.altafjava.school.application.service.EventService;

@RestController
@RequestMapping("/api/v1/events")
public class EventController implements EventApi {

	private final EventService eventService;
	private final EventMapper eventMapper;
	private final EventRegistrationService eventRegistrationService;
	private final EventRegistrationMapper eventRegistrationMapper;

	private final SpringDataPageableResolver pageableResolver;

	public EventController(EventService eventService, EventMapper eventMapper,
			EventRegistrationService eventRegistrationService, EventRegistrationMapper eventRegistrationMapper,
			SpringDataPageableResolver pageableResolver) {
		this.eventService = eventService;
		this.eventMapper = eventMapper;
		this.eventRegistrationService = eventRegistrationService;
		this.eventRegistrationMapper = eventRegistrationMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_READ')")
	@SortableBy({ "title", "eventDate" })
	public ApiResponse<com.altafjava.platform.core.model.Page<EventResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(eventService.list(pageableResolver.resolve(page, size)).map(eventMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_READ')")
	public ApiResponse<EventResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(eventMapper.toResponse(eventService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_MANAGE')")
	public ApiResponse<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
		return ApiResponse.success(eventMapper.toResponse(eventService.create(request.title(), request.description(),
				request.eventDate(), request.location(), request.registrationRequired(), request.capacity())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_MANAGE')")
	public ApiResponse<EventResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEventRequest request) {
		return ApiResponse.success(
				eventMapper.toResponse(eventService.updateDetails(publicId, request.title(), request.description(),
						request.eventDate(), request.location())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_MANAGE')")
	public ApiResponse<EventResponse> cancel(@PathVariable String publicId) {
		return ApiResponse.success(eventMapper.toResponse(eventService.cancel(publicId)));
	}

	@Override
	@GetMapping("/{publicId}/registrations")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_STAFF_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<EventRegistrationResponse>> listRegistrations(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(eventRegistrationService.listForEvent(publicId, pageableResolver.resolve(page, size))
						.map(eventRegistrationMapper::toResponse)));
	}

	@Override
	@PostMapping("/{publicId}/registrations")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_REGISTER')")
	public ApiResponse<EventRegistrationResponse> register(@PathVariable String publicId,
			@Valid @RequestBody RegisterForEventRequest request) {
		return ApiResponse.success(eventRegistrationMapper.toResponse(
				eventRegistrationService.register(publicId, request.studentPublicId())));
	}

	@Override
	@PatchMapping("/registrations/{registrationPublicId}/cancel")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_REGISTER')")
	public ApiResponse<EventRegistrationResponse> cancelRegistration(@PathVariable String registrationPublicId) {
		return ApiResponse
				.success(eventRegistrationMapper.toResponse(eventRegistrationService.cancel(registrationPublicId)));
	}

	@Override
	@PatchMapping("/registrations/{registrationPublicId}/attended")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EVENT_STAFF_MANAGE')")
	public ApiResponse<EventRegistrationResponse> markAttended(@PathVariable String registrationPublicId) {
		return ApiResponse.success(
				eventRegistrationMapper.toResponse(eventRegistrationService.markAttended(registrationPublicId)));
	}
}

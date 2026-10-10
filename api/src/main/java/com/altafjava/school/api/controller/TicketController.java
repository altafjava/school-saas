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
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.school.api.controller.api.TicketApi;
import com.altafjava.school.api.dto.request.AssignTicketRequest;
import com.altafjava.school.api.dto.request.RaiseTicketRequest;
import com.altafjava.school.api.dto.request.ResolveTicketRequest;
import com.altafjava.school.api.dto.response.TicketResponse;
import com.altafjava.school.api.mapper.TicketMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.TicketService;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;

/**
 * Raising a ticket ({@code POST}) and viewing one's own tickets ({@code GET /my}) are gated broadly
 * ({@code TENANT_ADMIN}/{@code TEACHER}/{@code PARENT}/{@code STUDENT}) — helpdesk support is meant
 * to reach the whole tenant population, unlike a narrowly-scoped staff workflow such as
 * {@code LeaveRequest.submit} (teacher-only). Triage operations (search all, view any ticket's
 * detail, assign, resolve, close, reopen) are gated to {@code TENANT_ADMIN_OR_TEACHER} — the closest
 * existing role to a support-desk function, mirroring Hostel/Visitor's operational gate (no dedicated
 * support-staff role exists in the seeded catalog).
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController implements TicketApi {

	private final TicketService ticketService;
	private final TicketMapper ticketMapper;

	private final SpringDataPageableResolver pageableResolver;

	public TicketController(TicketService ticketService, TicketMapper ticketMapper,
			SpringDataPageableResolver pageableResolver) {
		this.ticketService = ticketService;
		this.ticketMapper = ticketMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	@SortableBy({ "status", "category", "subject" })
	public ApiResponse<com.altafjava.platform.core.model.Page<TicketResponse>> search(
			@RequestParam(required = false) TicketStatus status,
			@RequestParam(required = false) TicketCategory category,
			@RequestParam(required = false) String assignedToUserPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				ticketService.search(status, category, assignedToUserPublicId, pageableResolver.resolve(page, size))
						.map(ticketMapper::toResponse)));
	}

	@Override
	@GetMapping("/my")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_SELF_SERVICE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<TicketResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				ticketService.listMine(pageableResolver.resolve(page, size)).map(ticketMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	public ApiResponse<TicketResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(ticketMapper.toResponse(ticketService.get(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_SELF_SERVICE')")
	public ApiResponse<TicketResponse> raise(@Valid @RequestBody RaiseTicketRequest request) {
		return ApiResponse.success(ticketMapper.toResponse(
				ticketService.raise(request.category(), request.subject(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}/assign")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	public ApiResponse<TicketResponse> assign(@PathVariable String publicId,
			@Valid @RequestBody AssignTicketRequest request) {
		return ApiResponse
				.success(ticketMapper.toResponse(ticketService.assign(publicId, request.assignedToUserPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/resolve")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	public ApiResponse<TicketResponse> resolve(@PathVariable String publicId,
			@Valid @RequestBody ResolveTicketRequest request) {
		return ApiResponse.success(ticketMapper.toResponse(ticketService.resolve(publicId, request.resolution())));
	}

	@Override
	@PatchMapping("/{publicId}/close")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	public ApiResponse<TicketResponse> close(@PathVariable String publicId) {
		return ApiResponse.success(ticketMapper.toResponse(ticketService.close(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/reopen")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TICKET_MANAGE')")
	public ApiResponse<TicketResponse> reopen(@PathVariable String publicId) {
		return ApiResponse.success(ticketMapper.toResponse(ticketService.reopen(publicId)));
	}
}

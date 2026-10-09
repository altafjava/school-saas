package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignTicketRequest;
import com.altafjava.school.api.dto.request.RaiseTicketRequest;
import com.altafjava.school.api.dto.request.ResolveTicketRequest;
import com.altafjava.school.api.dto.response.TicketResponse;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ticket", description = "APIs for managing Ticket operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface TicketApi {

	@Operation(summary = "Search")
	public ApiResponse<com.altafjava.platform.core.model.Page<TicketResponse>> search(
			@RequestParam(required = false) TicketStatus status,
			@RequestParam(required = false) TicketCategory category,
			@RequestParam(required = false) Long assignedToUserId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List mine")
	public ApiResponse<com.altafjava.platform.core.model.Page<TicketResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<TicketResponse> get(@PathVariable String publicId);

	@Operation(summary = "Raise")
	public ApiResponse<TicketResponse> raise(@Valid @RequestBody RaiseTicketRequest request);

	@Operation(summary = "Assign")
	public ApiResponse<TicketResponse> assign(@PathVariable String publicId,
			@Valid @RequestBody AssignTicketRequest request);

	@Operation(summary = "Resolve")
	public ApiResponse<TicketResponse> resolve(@PathVariable String publicId,
			@Valid @RequestBody ResolveTicketRequest request);

	@Operation(summary = "Close")
	public ApiResponse<TicketResponse> close(@PathVariable String publicId);

	@Operation(summary = "Reopen")
	public ApiResponse<TicketResponse> reopen(@PathVariable String publicId);
}

package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.AttachVisitorPhotoRequest;
import com.altafjava.school.api.dto.request.RaiseVisitorRequestRequest;
import com.altafjava.school.api.dto.request.RejectVisitorRequestRequest;
import com.altafjava.school.api.dto.response.VisitorRequestResponse;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Visitor Request", description = "APIs for managing Visitor Request operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface VisitorRequestApi {

	@Operation(summary = "List", operationId = "visitorrequest_list", description = "Every visit request in the school, optionally filtered by status and a visit-date range.")
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorRequestResponse>> list(
			@RequestParam(required = false) VisitorRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List hosted by me", operationId = "visitorrequest_listHostedByMe", description = "Visits where the current employee is the host.")
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorRequestResponse>> listHostedByMe(
			@RequestParam(required = false) VisitorRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Get", operationId = "visitorrequest_get")
	public ApiResponse<VisitorRequestResponse> get(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Raise", operationId = "visitorrequest_raise", description = "Pre-registers a visit for visitDate, or raises a walk-in for today when visitDate is omitted. "
			+ "Hosts can raise visits for themselves; the front desk for any host. The host is notified and the "
			+ "request waits, PENDING, for approval.")
	public ApiResponse<VisitorRequestResponse> raise(@Valid @RequestBody RaiseVisitorRequestRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Approve", operationId = "visitorrequest_approve", description = "By the host or a visitor-request approver. Only an approved visit for today can be checked in.")
	public ApiResponse<VisitorRequestResponse> approve(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Reject", operationId = "visitorrequest_reject")
	public ApiResponse<VisitorRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectVisitorRequestRequest request, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Cancel", operationId = "visitorrequest_cancel")
	public ApiResponse<VisitorRequestResponse> cancel(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Attach photo", operationId = "visitorrequest_attachPhoto", description = "Records the visitor's photo, uploaded beforehand through the file API, so it is ready at the gate.")
	public ApiResponse<VisitorRequestResponse> attachPhoto(@PathVariable String publicId,
			@Valid @RequestBody AttachVisitorPhotoRequest request, @AuthenticationPrincipal AuthenticatedUser user);
}

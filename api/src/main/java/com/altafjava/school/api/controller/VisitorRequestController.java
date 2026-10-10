package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.VisitorRequestApi;
import com.altafjava.school.api.dto.request.AttachVisitorPhotoRequest;
import com.altafjava.school.api.dto.request.RaiseVisitorRequestRequest;
import com.altafjava.school.api.dto.request.RejectVisitorRequestRequest;
import com.altafjava.school.api.dto.response.VisitorRequestResponse;
import com.altafjava.school.api.mapper.VisitorRequestMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.VisitorRequestService;
import com.altafjava.school.domain.visitor.model.VisitorRequestStatus;

/**
 * The front desk ({@code VISITOR_LOG_MANAGE}) and approvers ({@code VISITOR_REQUEST_APPROVE}) see
 * every request; hosts ({@code VISITOR_REQUEST_SELF_SERVICE}) only act on their own guests — the
 * service narrows each call to the right set.
 */
@RestController
@RequestMapping("/api/v1/visitor-requests")
public class VisitorRequestController implements VisitorRequestApi {

	private static final String DESK_OR_APPROVER = "@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE') "
			+ "or @permissionAuthorizationService.hasPermission('VISITOR_REQUEST_APPROVE')";
	private static final String ANY_PARTICIPANT = DESK_OR_APPROVER
			+ " or @permissionAuthorizationService.hasPermission('VISITOR_REQUEST_SELF_SERVICE')";
	private static final String HOST_OR_APPROVER = "@permissionAuthorizationService.hasPermission('VISITOR_REQUEST_APPROVE') "
			+ "or @permissionAuthorizationService.hasPermission('VISITOR_REQUEST_SELF_SERVICE')";

	private final VisitorRequestService visitorRequestService;
	private final VisitorRequestMapper visitorRequestMapper;
	private final SpringDataPageableResolver pageableResolver;

	public VisitorRequestController(VisitorRequestService visitorRequestService,
			VisitorRequestMapper visitorRequestMapper, SpringDataPageableResolver pageableResolver) {
		this.visitorRequestService = visitorRequestService;
		this.visitorRequestMapper = visitorRequestMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize(DESK_OR_APPROVER)
	@SortableBy({ "visitDate", "status", "source" })
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorRequestResponse>> list(
			@RequestParam(required = false) VisitorRequestStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				visitorRequestService.list(status, from, to, pageableResolver.resolve(page, size))
						.map(visitorRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/hosted-by-me")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_REQUEST_SELF_SERVICE')")
	@SortableBy({ "visitDate", "status", "source" })
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorRequestResponse>> listHostedByMe(
			@RequestParam(required = false) VisitorRequestStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(visitorRequestService
				.listHostedByCurrentEmployee(user.getId(), status, from, to, pageableResolver.resolve(page, size))
				.map(visitorRequestMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize(ANY_PARTICIPANT)
	public ApiResponse<VisitorRequestResponse> get(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(visitorRequestMapper.toResponse(visitorRequestService.findByPublicId(publicId, user.getId())));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE') "
			+ "or @permissionAuthorizationService.hasPermission('VISITOR_REQUEST_SELF_SERVICE')")
	public ApiResponse<VisitorRequestResponse> raise(@Valid @RequestBody RaiseVisitorRequestRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(visitorRequestMapper.toResponse(visitorRequestService.raise(request.visitorName(),
				request.visitorPhone(), request.purpose(), request.hostEmployeePublicId(), request.visitDate(),
				user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/approve")
	@PreAuthorize(HOST_OR_APPROVER)
	public ApiResponse<VisitorRequestResponse> approve(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(visitorRequestMapper.toResponse(visitorRequestService.approve(publicId, user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/reject")
	@Command
	@PreAuthorize(HOST_OR_APPROVER)
	public ApiResponse<VisitorRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectVisitorRequestRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(visitorRequestMapper
				.toResponse(visitorRequestService.reject(publicId, request.reason(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@PreAuthorize(ANY_PARTICIPANT)
	public ApiResponse<VisitorRequestResponse> cancel(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(visitorRequestMapper.toResponse(visitorRequestService.cancel(publicId, user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/photo")
	@Command
	@PreAuthorize(ANY_PARTICIPANT)
	public ApiResponse<VisitorRequestResponse> attachPhoto(@PathVariable String publicId,
			@Valid @RequestBody AttachVisitorPhotoRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(visitorRequestMapper
				.toResponse(visitorRequestService.attachPhoto(publicId, request.photoFilePublicId(), user.getId())));
	}
}

package com.altafjava.school.api.controller;

import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.VisitorLogApi;
import com.altafjava.school.api.dto.request.CheckInVisitorRequest;
import com.altafjava.school.api.dto.response.VisitorLogResponse;
import com.altafjava.school.api.mapper.VisitorLogMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.VisitorLogService;

/**
 * Gated to the {@code VISITOR_LOG_MANAGE} permission, seeded onto {@code TEACHER} by default —
 * a tenant admin can grant it to a dedicated front-desk/receptionist role instead if one is
 * defined. Kept authenticated rather than {@code permitAll()} since visitor logs are internal
 * building-security data, unlike the certificate-verification or admission-application public
 * endpoints. A visitor is only checked in against an approved visitor request.
 */
@RestController
@RequestMapping("/api/v1/visitor-logs")
public class VisitorLogController implements VisitorLogApi {

	private final VisitorLogService visitorLogService;
	private final VisitorLogMapper visitorLogMapper;

	private final SpringDataPageableResolver pageableResolver;

	public VisitorLogController(VisitorLogService visitorLogService, VisitorLogMapper visitorLogMapper,
			SpringDataPageableResolver pageableResolver) {
		this.visitorLogService = visitorLogService;
		this.visitorLogMapper = visitorLogMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE')")
	@SortableBy({ "checkInAt", "checkOutAt" })
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorLogResponse>> list(
			@RequestParam(required = false) Boolean stillCheckedIn,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(visitorLogService.list(stillCheckedIn, from, to, pageableResolver.resolve(page, size))
						.map(visitorLogMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE')")
	public ApiResponse<VisitorLogResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(visitorLogMapper.toResponse(visitorLogService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE')")
	public ApiResponse<VisitorLogResponse> checkIn(@Valid @RequestBody CheckInVisitorRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(visitorLogMapper.toResponse(visitorLogService.checkIn(
				request.visitorRequestPublicId(), request.photoFilePublicId(), user.getId())));
	}

	// Not ApiResponse-wrapped — a raw PDF download, like the ID card downloads.
	@Override
	@GetMapping("/{publicId}/badge")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE')")
	public ResponseEntity<byte[]> downloadBadge(@PathVariable String publicId) {
		byte[] pdf = visitorLogService.downloadBadgePdf(visitorLogService.findBadge(publicId));
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(publicId + "-badge.pdf").build().toString())
				.body(pdf);
	}

	@Override
	@PatchMapping("/{publicId}/check-out")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('VISITOR_LOG_MANAGE')")
	public ApiResponse<VisitorLogResponse> checkOut(@PathVariable String publicId) {
		return ApiResponse.success(visitorLogMapper.toResponse(visitorLogService.checkOut(publicId)));
	}
}

package com.altafjava.school.api.controller.api;

import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.CheckInVisitorRequest;
import com.altafjava.school.api.dto.response.VisitorLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Visitor Log", description = "APIs for managing Visitor Log operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface VisitorLogApi {

	@Operation(summary = "List", operationId = "visitorlog_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<VisitorLogResponse>> list(
			@RequestParam(required = false) Boolean stillCheckedIn,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "visitorlog_get")
	public ApiResponse<VisitorLogResponse> get(@PathVariable String publicId);

	@Operation(summary = "Check in", operationId = "visitorlog_checkIn", description = "Admits a visitor against an APPROVED visitor request for today. A photo is required — "
			+ "either already on the request or supplied here — and a badge is issued; it stops being valid at "
			+ "check-out.")
	public ApiResponse<VisitorLogResponse> checkIn(@Valid @RequestBody CheckInVisitorRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Download badge", operationId = "visitorlog_downloadBadge", description = "The visitor's badge as a PDF.")
	public ResponseEntity<byte[]> downloadBadge(@PathVariable String publicId);

	@Operation(summary = "Check out", operationId = "visitorlog_checkOut")
	public ApiResponse<VisitorLogResponse> checkOut(@PathVariable String publicId);
}

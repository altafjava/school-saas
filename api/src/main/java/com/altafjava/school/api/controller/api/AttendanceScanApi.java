package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.ScanAttendanceRequest;
import com.altafjava.school.api.dto.response.AttendanceScanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Attendance Scan", description = "APIs for managing Attendance Scan operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AttendanceScanApi {

	@Operation(summary = "Scan ID card", description = "Marks the student on a scanned ID card present for today. payload is whatever the "
			+ "scanner read from the QR code (the card's verification link or bare code). A revoked, non-student or "
			+ "unenrolled card is refused; a repeat scan the same day returns the existing record with "
			+ "alreadyMarked=true.")
	public ApiResponse<AttendanceScanResponse> scan(@Valid @RequestBody ScanAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);
}

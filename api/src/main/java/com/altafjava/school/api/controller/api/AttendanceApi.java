package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.MarkAttendanceRequest;
import com.altafjava.school.api.dto.request.UpdateAttendanceStatusRequest;
import com.altafjava.school.api.dto.response.AttendanceCorrectionResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Attendance", description = "APIs for managing Attendance operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AttendanceApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) AttendanceStatus status);

	@Operation(summary = "Get")
	public ApiResponse<AttendanceResponse> get(@PathVariable String publicId);

	@Operation(summary = "Mark")
	public ApiResponse<AttendanceResponse> mark(@Valid @RequestBody MarkAttendanceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Update status")
	public ApiResponse<AttendanceResponse> updateStatus(@PathVariable String publicId,
			@Valid @RequestBody UpdateAttendanceStatusRequest request);

	@Operation(summary = "List corrections")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceCorrectionResponse>> listCorrections(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Delete")
	public ApiResponse<Void> delete(@PathVariable String publicId);
}

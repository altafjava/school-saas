package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ConfigureLeaveApprovalRequest;
import com.altafjava.school.api.dto.request.ConfigureLeaveCarryForwardRequest;
import com.altafjava.school.api.dto.request.CreateLeaveTypeRequest;
import com.altafjava.school.api.dto.request.UpdateLeaveTypeRequest;
import com.altafjava.school.api.dto.response.LeaveTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Leave Type", description = "APIs for managing Leave Type operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LeaveTypeApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveTypeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "List active")
	public ApiResponse<List<LeaveTypeResponse>> listActive();

	@Operation(summary = "Get")
	public ApiResponse<LeaveTypeResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<LeaveTypeResponse> create(@Valid @RequestBody CreateLeaveTypeRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<LeaveTypeResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateLeaveTypeRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<LeaveTypeResponse> deactivate(@PathVariable String publicId);

	@Operation(summary = "Mark unpaid")
	public ApiResponse<LeaveTypeResponse> markUnpaid(@PathVariable String publicId);

	@Operation(summary = "Mark paid")
	public ApiResponse<LeaveTypeResponse> markPaid(@PathVariable String publicId);

	@Operation(summary = "Restrict during probation")
	public ApiResponse<LeaveTypeResponse> restrictDuringProbation(@PathVariable String publicId);

	@Operation(summary = "Allow during probation")
	public ApiResponse<LeaveTypeResponse> allowDuringProbation(@PathVariable String publicId);

	@Operation(summary = "Configure carry forward")
	public ApiResponse<LeaveTypeResponse> configureCarryForward(@PathVariable String publicId,
			@Valid @RequestBody ConfigureLeaveCarryForwardRequest request);

	@Operation(summary = "Configure approval levels", description = "1 leaves the decision to a leave administrator; 2 makes the requester's "
			+ "department head approve first. Applies to requests submitted afterwards.")
	public ApiResponse<LeaveTypeResponse> configureApprovalLevels(@PathVariable String publicId,
			@Valid @RequestBody ConfigureLeaveApprovalRequest request);
}

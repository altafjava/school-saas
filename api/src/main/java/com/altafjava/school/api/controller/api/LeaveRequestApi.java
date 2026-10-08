package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitLeaveRequestRequest;
import com.altafjava.school.api.dto.response.LeaveRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Leave Request", description = "APIs for managing Leave Request operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LeaveRequestApi {

	@Operation(summary = "List", operationId = "leaverequest_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List mine", operationId = "leaverequest_listMine", description = "Lists the current employee's own leave requests.")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Submit", operationId = "leaverequest_submit", description = "Requests leave for a date range. Days requested exclude holidays and are validated "
			+ "against the leave type's probation-eligibility rule for the requesting employee.")
	public ApiResponse<LeaveRequestResponse> submit(@Valid @RequestBody SubmitLeaveRequestRequest request);

	@Operation(summary = "Approve", operationId = "leaverequest_approve", description = "Approves the request and atomically deducts the days from the employee's leave balance "
			+ "for that type and academic year.")
	public ApiResponse<LeaveRequestResponse> approve(@PathVariable String publicId);

	@Operation(summary = "Reject", operationId = "leaverequest_reject")
	public ApiResponse<LeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request);

	@Operation(summary = "Cancel", operationId = "leaverequest_cancel")
	public ApiResponse<LeaveRequestResponse> cancel(@PathVariable String publicId);
}

package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitLeaveRequestRequest;
import com.altafjava.school.api.dto.response.LeaveApprovalResponse;
import com.altafjava.school.api.dto.response.LeaveRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Leave Request", description = "APIs for managing Leave Request operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LeaveRequestApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List mine", description = "Lists the current employee's own leave requests.")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listMine(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Submit", description = "Requests leave for a date range. Days requested exclude holidays and are validated "
			+ "against the leave type's probation-eligibility rule for the requesting employee.")
	public ApiResponse<LeaveRequestResponse> submit(@Valid @RequestBody SubmitLeaveRequestRequest request);

	@Operation(summary = "List awaiting my review", description = "Requests waiting for the current employee's approval as head of the "
			+ "requester's department.")
	public ApiResponse<com.altafjava.platform.core.model.Page<LeaveRequestResponse>> listAwaitingMyReview(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List approvals", description = "The decision trail of one request: who decided at which level, and when.")
	public ApiResponse<List<LeaveApprovalResponse>> listApprovals(@PathVariable String publicId);

	@Operation(summary = "Approve", description = "Records the caller's approval for the level the request is waiting on: the "
			+ "requester's department head first when the leave type asks for it, then a leave administrator. "
			+ "The final approval atomically deducts the days from the employee's leave balance for that "
			+ "type and academic year. Nobody can decide their own request or approve at two levels.")
	public ApiResponse<LeaveRequestResponse> approve(@PathVariable String publicId);

	@Operation(summary = "Reject")
	public ApiResponse<LeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request);

	@Operation(summary = "Cancel")
	public ApiResponse<LeaveRequestResponse> cancel(@PathVariable String publicId);
}

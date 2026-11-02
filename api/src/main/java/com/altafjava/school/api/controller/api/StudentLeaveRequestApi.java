package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.RejectLeaveRequestRequest;
import com.altafjava.school.api.dto.request.SubmitStudentLeaveRequest;
import com.altafjava.school.api.dto.response.StudentLeaveRequestResponse;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Leave Request", description = "Leave of absence for students, raised by guardians or staff and decided by the class teacher.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentLeaveRequestApi {

	@Operation(summary = "List", description = "Staff see the classrooms they reach, guardians their linked children.")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentLeaveRequestResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) LeaveRequestStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to);

	@Operation(summary = "Get")
	public ApiResponse<StudentLeaveRequestResponse> get(@PathVariable String publicId);

	@Operation(summary = "Submit")
	public ApiResponse<StudentLeaveRequestResponse> submit(@Valid @RequestBody SubmitStudentLeaveRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Approve", description = "Marks the school days of the leave as excused. Present and late marks are kept.")
	public ApiResponse<StudentLeaveRequestResponse> approve(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Reject")
	public ApiResponse<StudentLeaveRequestResponse> reject(@PathVariable String publicId,
			@Valid @RequestBody RejectLeaveRequestRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Cancel", description = "Only a pending request can be cancelled, by its requester or a leave administrator.")
	public ApiResponse<StudentLeaveRequestResponse> cancel(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);
}

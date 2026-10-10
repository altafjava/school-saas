package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateAssignmentRequest;
import com.altafjava.school.api.dto.request.RescheduleAssignmentRequest;
import com.altafjava.school.api.dto.response.AssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Assignment", description = "APIs for managing Assignment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AssignmentApi {

	@Operation(summary = "Create")
	public ApiResponse<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request);

	@Operation(summary = "List", description = "Filter by classroom, subject and a date range (due date for assignments, posting date for lessons). Without a classroom, covers the classrooms the caller can reach.")
	public ApiResponse<com.altafjava.platform.core.model.Page<AssignmentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to);

	@Operation(summary = "Reschedule")
	public ApiResponse<AssignmentResponse> reschedule(@PathVariable String publicId,
			@Valid @RequestBody RescheduleAssignmentRequest request);
}

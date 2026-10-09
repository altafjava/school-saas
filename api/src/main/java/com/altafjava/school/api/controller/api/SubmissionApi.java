package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.GradeSubmissionRequest;
import com.altafjava.school.api.dto.request.SubmitAssignmentRequest;
import com.altafjava.school.api.dto.response.SubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Submission", description = "APIs for managing Submission operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface SubmissionApi {

	@Operation(summary = "Submit")
	public ApiResponse<SubmissionResponse> submit(@PathVariable String assignmentPublicId,
			@Valid @RequestBody SubmitAssignmentRequest request);

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<SubmissionResponse>> list(
			@PathVariable String assignmentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Grade")
	public ApiResponse<SubmissionResponse> grade(@PathVariable String assignmentPublicId,
			@PathVariable String submissionPublicId, @Valid @RequestBody GradeSubmissionRequest request);
}

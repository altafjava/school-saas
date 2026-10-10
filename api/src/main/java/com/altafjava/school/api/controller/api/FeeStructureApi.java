package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignFeeStructureRequest;
import com.altafjava.school.api.dto.request.ConfigureFeeAssignmentDueDateRequest;
import com.altafjava.school.api.dto.request.ConfigureFeeLateFeePolicyRequest;
import com.altafjava.school.api.dto.request.CreateFeeStructureRequest;
import com.altafjava.school.api.dto.request.ReviseFeeAmountRequest;
import com.altafjava.school.api.dto.response.FeeAssignmentResponse;
import com.altafjava.school.api.dto.response.FeeStructureResponse;
import com.altafjava.school.api.dto.response.FeeStructureRevisionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Structure", description = "APIs for managing Fee Structure operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface FeeStructureApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeStructureResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<FeeStructureResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<FeeStructureResponse> create(@Valid @RequestBody CreateFeeStructureRequest request);

	@Operation(summary = "Revise amount")
	public ApiResponse<FeeStructureResponse> reviseAmount(@PathVariable String publicId,
			@Valid @RequestBody ReviseFeeAmountRequest request);

	@Operation(summary = "Configure late fee policy")
	public ApiResponse<FeeStructureResponse> configureLateFeePolicy(@PathVariable String publicId,
			@Valid @RequestBody ConfigureFeeLateFeePolicyRequest request);

	@Operation(summary = "List revisions")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeStructureRevisionResponse>> listRevisions(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Assign")
	public ApiResponse<FeeAssignmentResponse> assign(@PathVariable String publicId,
			@Valid @RequestBody AssignFeeStructureRequest request);

	@Operation(summary = "List assignments")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeAssignmentResponse>> listAssignments(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Revoke assignment")
	public ApiResponse<Void> revokeAssignment(@PathVariable String publicId, @PathVariable String assignmentPublicId);

	@Operation(summary = "Configure assignment due date")
	public ApiResponse<FeeAssignmentResponse> configureAssignmentDueDate(@PathVariable String publicId,
			@PathVariable String assignmentPublicId, @Valid @RequestBody ConfigureFeeAssignmentDueDateRequest request);
}

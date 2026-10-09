package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.DecideAdmissionRequest;
import com.altafjava.school.api.dto.request.PublicAdmissionApplicationRequest;
import com.altafjava.school.api.dto.request.RecordEntranceTestScoreRequest;
import com.altafjava.school.api.dto.request.SubmitAdmissionRequest;
import com.altafjava.school.api.dto.request.WaiveApplicationFeeRequest;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admission", description = "APIs for managing Admission operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AdmissionApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<AdmissionResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<AdmissionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Submit")
	public ApiResponse<AdmissionResponse> submit(@Valid @RequestBody SubmitAdmissionRequest request);

	/**
	 * Public, unauthenticated intake for a prospective guardian applying before any account
	 * exists — no {@code @PreAuthorize}, mirroring platform's {@code AuthController.register()}.
	 */
	@Operation(summary = "Apply")
	public ApiResponse<AdmissionResponse> apply(@Valid @RequestBody PublicAdmissionApplicationRequest request);

	@Operation(summary = "Mark under review")
	public ApiResponse<AdmissionResponse> markUnderReview(@PathVariable String publicId);

	/**
	 * A REJECTED outcome takes effect immediately; an APPROVED outcome submits to the tenant's
	 * {@code ADMISSION_DECISION} workflow instead, returning 202 with the approval request's ID
	 * rather than the 200 {@link AdmissionResponse} below. The studentCode check happens here, not
	 * in {@code requestApproval}, since {@code ApprovalAspect} intercepts that call before its body
	 * ever runs once a workflow is configured.
	 */
	@Operation(summary = "Decide")
	public ApiResponse<AdmissionResponse> decide(@PathVariable String publicId,
			@Valid @RequestBody DecideAdmissionRequest request);

	@Operation(summary = "Record entrance test score")
	public ApiResponse<AdmissionResponse> recordEntranceTestScore(@PathVariable String publicId,
			@Valid @RequestBody RecordEntranceTestScoreRequest request);

	@Operation(summary = "Generate merit list")
	public ApiResponse<List<AdmissionResponse>> generateMeritList(
			@RequestParam String appliedGrade,
			@RequestParam int availableSeats);

	@Operation(summary = "Promote from waitlist")
	public ApiResponse<AdmissionResponse> promoteFromWaitlist(@PathVariable String publicId);

	@Operation(summary = "Record the application fee as paid")
	public ApiResponse<AdmissionResponse> recordApplicationFee(@PathVariable String publicId);

	@Operation(summary = "Waive the application fee")
	public ApiResponse<AdmissionResponse> waiveApplicationFee(@PathVariable String publicId,
			@Valid @RequestBody WaiveApplicationFeeRequest request);

	@Operation(summary = "Issue or re-issue the offer letter")
	public ApiResponse<AdmissionResponse> issueOfferLetter(@PathVariable String publicId);

	@Operation(summary = "Download the current offer letter")
	public ResponseEntity<byte[]> downloadOfferLetter(@PathVariable String publicId);
}

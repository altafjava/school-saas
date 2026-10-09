package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ReferForCounselingRequest;
import com.altafjava.school.api.dto.request.ScheduleCounselingReferralRequest;
import com.altafjava.school.api.dto.response.CounselingReferralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Counseling Referral", description = "APIs for managing Counseling Referral operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CounselingReferralApi {

	@Operation(summary = "List all")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingReferralResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List for student")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingReferralResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<CounselingReferralResponse> get(@PathVariable String publicId);

	@Operation(summary = "Refer")
	public ApiResponse<CounselingReferralResponse> refer(@Valid @RequestBody ReferForCounselingRequest request);

	@Operation(summary = "Schedule")
	public ApiResponse<CounselingReferralResponse> schedule(@PathVariable String publicId,
			@Valid @RequestBody ScheduleCounselingReferralRequest request);

	@Operation(summary = "Complete")
	public ApiResponse<CounselingReferralResponse> complete(@PathVariable String publicId);

	@Operation(summary = "Decline")
	public ApiResponse<CounselingReferralResponse> decline(@PathVariable String publicId);
}

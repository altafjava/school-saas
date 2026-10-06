package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateGuardianRequest;
import com.altafjava.school.api.dto.request.GrantGuardianConsentRequest;
import com.altafjava.school.api.dto.request.LinkGuardianRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.response.GuardianConsentRecordResponse;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Guardian", description = "APIs for managing Guardian operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface GuardianApi {

	@Operation(summary = "List", operationId = "guardian_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<GuardianResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "guardian_get")
	public ApiResponse<GuardianResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "guardian_create")
	public ApiResponse<GuardianResponse> create(@Valid @RequestBody CreateGuardianRequest request);

	@Operation(summary = "Update address", operationId = "guardian_updateAddress")
	public ApiResponse<GuardianResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request);

	@Operation(summary = "Update phone", operationId = "guardian_updatePhone")
	public ApiResponse<GuardianResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request);

	@Operation(summary = "Update photo", operationId = "guardian_updatePhoto")
	public ApiResponse<GuardianResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request);

	@Operation(summary = "Link student", operationId = "guardian_linkStudent")
	public ApiResponse<StudentGuardianLinkResponse> linkStudent(@PathVariable String publicId,
			@Valid @RequestBody LinkGuardianRequest request);

	@Operation(summary = "Grant consent", operationId = "guardian_grantConsent")
	public ApiResponse<StudentGuardianLinkResponse> grantConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId);

	@Operation(summary = "Revoke consent", operationId = "guardian_revokeConsent")
	public ApiResponse<StudentGuardianLinkResponse> revokeConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId);

	@Operation(summary = "My students", operationId = "guardian_myStudents")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> myStudents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Grant a data-processing consent for my linked student", operationId = "guardian_grantSelfConsent")
	public ApiResponse<GuardianConsentRecordResponse> grantSelfConsent(@PathVariable String studentPublicId,
			@Valid @RequestBody GrantGuardianConsentRequest request);

	@Operation(summary = "Revoke a previously granted data-processing consent for my linked student", operationId = "guardian_revokeSelfConsent")
	public ApiResponse<GuardianConsentRecordResponse> revokeSelfConsent(@PathVariable String studentPublicId,
			@PathVariable GuardianConsentType consentType);

	@Operation(summary = "List my own consent records for a linked student", operationId = "guardian_myConsents")
	public ApiResponse<List<GuardianConsentRecordResponse>> myConsents(@PathVariable String studentPublicId);

	@Operation(summary = "List a student's guardian consent records (admin view)", operationId = "guardian_studentConsents")
	public ApiResponse<List<GuardianConsentRecordResponse>> studentConsents(@PathVariable String studentPublicId);
}

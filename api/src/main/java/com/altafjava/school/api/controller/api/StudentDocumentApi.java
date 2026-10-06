package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.RejectDocumentRequest;
import com.altafjava.school.api.dto.request.UploadStudentDocumentRequest;
import com.altafjava.school.api.dto.response.StudentDocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Documents", description = "APIs for uploading and verifying student record documents (birth certificate, transfer certificate, ID proof, ...).\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentDocumentApi {

	@Operation(summary = "List", operationId = "studentdocument_list")
	ApiResponse<com.altafjava.platform.core.model.Page<StudentDocumentResponse>> list(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Upload", operationId = "studentdocument_upload")
	ApiResponse<StudentDocumentResponse> upload(@PathVariable String studentPublicId,
			@Valid @RequestBody UploadStudentDocumentRequest request);

	@Operation(summary = "Verify", operationId = "studentdocument_verify")
	ApiResponse<StudentDocumentResponse> verify(@PathVariable String studentPublicId,
			@PathVariable String documentPublicId, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Reject", operationId = "studentdocument_reject")
	ApiResponse<StudentDocumentResponse> reject(@PathVariable String studentPublicId,
			@PathVariable String documentPublicId, @Valid @RequestBody RejectDocumentRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);
}

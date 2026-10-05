package com.altafjava.school.api.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.IdCardIssuanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student ID Card", description = "Issues and downloads a student ID card via the platform's Document Template Engine.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentIdCardApi {

	@Operation(summary = "Issue", operationId = "studentidcard_issue")
	ApiResponse<IdCardIssuanceResponse> issue(@PathVariable String studentPublicId);

	@Operation(summary = "Download", operationId = "studentidcard_download")
	ResponseEntity<byte[]> download(@PathVariable String studentPublicId, @PathVariable String issuancePublicId);
}

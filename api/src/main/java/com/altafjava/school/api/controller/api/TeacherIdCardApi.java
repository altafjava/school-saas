package com.altafjava.school.api.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.IdCardIssuanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Teacher ID Card", description = "Issues and downloads a teacher ID card via the platform's Document Template Engine.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface TeacherIdCardApi {

	@Operation(summary = "Issue", operationId = "teacheridcard_issue")
	ApiResponse<IdCardIssuanceResponse> issue(@PathVariable String teacherPublicId);

	@Operation(summary = "Download", operationId = "teacheridcard_download")
	ResponseEntity<byte[]> download(@PathVariable String teacherPublicId, @PathVariable String issuancePublicId);
}

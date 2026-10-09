package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ScheduleCounselingSessionRequest;
import com.altafjava.school.api.dto.request.UpdateCounselingSessionNotesRequest;
import com.altafjava.school.api.dto.response.CounselingSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Counseling Session", description = "APIs for managing Counseling Session operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CounselingSessionApi {

	@Operation(summary = "List all")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List for student")
	public ApiResponse<com.altafjava.platform.core.model.Page<CounselingSessionResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get")
	public ApiResponse<CounselingSessionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Schedule")
	public ApiResponse<CounselingSessionResponse> schedule(
			@Valid @RequestBody ScheduleCounselingSessionRequest request);

	@Operation(summary = "Update notes")
	public ApiResponse<CounselingSessionResponse> updateNotes(@PathVariable String publicId,
			@Valid @RequestBody UpdateCounselingSessionNotesRequest request);
}

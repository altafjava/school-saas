package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.RecordMedicalIncidentRequest;
import com.altafjava.school.api.dto.response.MedicalIncidentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Medical Incident", description = "APIs for managing Medical Incident operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface MedicalIncidentApi {

	@Operation(summary = "List all")
	public ApiResponse<com.altafjava.platform.core.model.Page<MedicalIncidentResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to);

	@Operation(summary = "List for student")
	public ApiResponse<com.altafjava.platform.core.model.Page<MedicalIncidentResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Record")
	public ApiResponse<MedicalIncidentResponse> record(@Valid @RequestBody RecordMedicalIncidentRequest request);
}

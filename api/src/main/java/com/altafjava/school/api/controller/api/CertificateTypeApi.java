package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateCertificateTypeRequest;
import com.altafjava.school.api.dto.request.UpdateCertificateTypeRequest;
import com.altafjava.school.api.dto.response.CertificateTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Certificate Type", description = "APIs for managing Certificate Type operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CertificateTypeApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<CertificateTypeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "List active")
	public ApiResponse<List<CertificateTypeResponse>> listActive();

	@Operation(summary = "Get")
	public ApiResponse<CertificateTypeResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<CertificateTypeResponse> create(
			@Valid @RequestBody CreateCertificateTypeRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<CertificateTypeResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCertificateTypeRequest request);

	@Operation(summary = "Activate")
	public ApiResponse<CertificateTypeResponse> activate(@PathVariable String publicId);

	@Operation(summary = "Deactivate")
	public ApiResponse<CertificateTypeResponse> deactivate(@PathVariable String publicId);
}

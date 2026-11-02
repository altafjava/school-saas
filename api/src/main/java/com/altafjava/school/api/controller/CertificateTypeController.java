package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.feature.annotation.FeatureEnabled;
import com.altafjava.school.api.controller.api.CertificateTypeApi;
import com.altafjava.school.api.dto.request.CreateCertificateTypeRequest;
import com.altafjava.school.api.dto.request.UpdateCertificateTypeRequest;
import com.altafjava.school.api.dto.response.CertificateTypeResponse;
import com.altafjava.school.api.mapper.CertificateTypeMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.security.SchoolFeatures;
import com.altafjava.school.application.service.CertificateTypeService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

// Tenant-admin-defined certificate types (e.g. "Bonafide Certificate", "Transfer Certificate").
// No dedicated REGISTRAR role exists in this codebase, so "admin/registrar" access is granted to
// TENANT_ADMIN (full CRUD) and PRINCIPAL (read-only), the closest existing school-admin role,
// mirroring how PRINCIPAL is already used for other admin-adjacent read surfaces.
@RestController
@FeatureEnabled(SchoolFeatures.CERTIFICATES)
@RequestMapping("/api/v1/certificate-types")
public class CertificateTypeController implements CertificateTypeApi {

	private final CertificateTypeService certificateTypeService;
	private final CertificateTypeMapper certificateTypeMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CertificateTypeController(CertificateTypeService certificateTypeService,
			CertificateTypeMapper certificateTypeMapper, SpringDataPageableResolver pageableResolver) {
		this.certificateTypeService = certificateTypeService;
		this.certificateTypeMapper = certificateTypeMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<CertificateTypeResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(certificateTypeService.list(q, pageableResolver.resolve(page, size))
						.map(certificateTypeMapper::toResponse)));
	}

	@Override
	@GetMapping("/active")
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_READ)
	public ApiResponse<List<CertificateTypeResponse>> listActive() {
		return ApiResponse.success(
				certificateTypeService.listActive().stream().map(certificateTypeMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_READ)
	public ApiResponse<CertificateTypeResponse> get(@PathVariable String publicId) {
		return ApiResponse
				.success(certificateTypeMapper.toResponse(certificateTypeService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_WRITE)
	public ApiResponse<CertificateTypeResponse> create(
			@Valid @RequestBody CreateCertificateTypeRequest request) {
		return ApiResponse.success(certificateTypeMapper
				.toResponse(certificateTypeService.create(request.code(), request.name(), request.wording())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_WRITE)
	public ApiResponse<CertificateTypeResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateCertificateTypeRequest request) {
		return ApiResponse.success(certificateTypeMapper.toResponse(
				certificateTypeService.updateDetails(publicId, request.name(), request.wording(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/activate")
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_WRITE)
	public ApiResponse<CertificateTypeResponse> activate(@PathVariable String publicId) {
		return ApiResponse.success(certificateTypeMapper.toResponse(certificateTypeService.activate(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@RequiresPermission(SchoolPermissions.CERTIFICATE_TYPE_WRITE)
	public ApiResponse<CertificateTypeResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse
				.success(certificateTypeMapper.toResponse(certificateTypeService.deactivate(publicId)));
	}
}

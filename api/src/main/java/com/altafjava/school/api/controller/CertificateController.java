package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.api.controller.api.CertificateApi;
import com.altafjava.school.api.dto.request.RevokeDocumentRequest;
import com.altafjava.school.api.dto.response.IssuedDocumentResponse;
import com.altafjava.school.api.mapper.IssuedDocumentMapper;
import com.altafjava.school.api.ratelimit.RateLimited;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.CertificateService;

// Issuance is admin/registrar-only — unlike report cards, a certificate is never self-service
// for a parent/student, it is always issued by staff on request.
@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/certificates")
public class CertificateController implements CertificateApi {

	private final CertificateService certificateService;
	private final IssuedDocumentMapper issuedDocumentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public CertificateController(CertificateService certificateService,
			IssuedDocumentMapper issuedDocumentMapper, SpringDataPageableResolver pageableResolver) {
		this.certificateService = certificateService;
		this.issuedDocumentMapper = issuedDocumentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<IssuedDocumentResponse>> list(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				certificateService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(issuedDocumentMapper::toResponse));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	@RateLimited(key = "certificate-issue", capacity = 60, periodMinutes = 60)
	public ApiResponse<IssuedDocumentResponse> issue(@PathVariable String studentPublicId,
			@RequestParam String certificateTypePublicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		DocumentIssuance issuance = certificateService.issue(studentPublicId, certificateTypePublicId,
				user.getId());
		return ApiResponse.success(issuedDocumentMapper.toResponse(issuance));
	}

	@Override
	@PatchMapping("/{certificatePublicId}/revoke")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public ApiResponse<IssuedDocumentResponse> revoke(@PathVariable String studentPublicId,
			@PathVariable String certificatePublicId, @Valid @RequestBody RevokeDocumentRequest request) {
		return ApiResponse.success(issuedDocumentMapper
				.toResponse(certificateService.revoke(studentPublicId, certificatePublicId, request.reason())));
	}

	// Not ApiResponse-wrapped, unlike every other endpoint in this codebase — a raw PDF download
	// needs its own Content-Type/Content-Disposition headers and binary body, not a JSON envelope.
	@Override
	@GetMapping("/{certificatePublicId}/download")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CERTIFICATE_MANAGE')")
	public ResponseEntity<byte[]> download(@PathVariable String studentPublicId,
			@PathVariable String certificatePublicId) {
		DocumentIssuance issuance = certificateService.findByPublicId(studentPublicId, certificatePublicId);
		byte[] pdf = certificateService.downloadPdf(issuance);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(certificatePublicId + ".pdf").build().toString())
				.body(pdf);
	}
}

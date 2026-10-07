package com.altafjava.school.api.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.api.controller.api.TeacherIdCardApi;
import com.altafjava.school.api.dto.response.IssuedDocumentResponse;
import com.altafjava.school.api.mapper.IssuedDocumentMapper;
import com.altafjava.school.api.ratelimit.RateLimited;
import com.altafjava.school.application.idcard.IdCardService;

@RestController
@RequestMapping("/api/v1/teachers/{teacherPublicId}/id-card")
public class TeacherIdCardController implements TeacherIdCardApi {

	private final IdCardService idCardService;
	private final IssuedDocumentMapper issuedDocumentMapper;

	public TeacherIdCardController(IdCardService idCardService, IssuedDocumentMapper issuedDocumentMapper) {
		this.idCardService = idCardService;
		this.issuedDocumentMapper = issuedDocumentMapper;
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ID_CARD_ISSUE')")
	@RateLimited(key = "teacher-id-card-issue", capacity = 60, periodMinutes = 60)
	public ApiResponse<IssuedDocumentResponse> issue(@PathVariable String teacherPublicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		DocumentIssuance issuance = idCardService.issueForTeacher(teacherPublicId, user.getId());
		return ApiResponse.success(issuedDocumentMapper.toResponse(issuance));
	}

	// Not ApiResponse-wrapped, unlike every other endpoint in this codebase — a raw PDF download
	// needs its own Content-Type/Content-Disposition headers and binary body, not a JSON envelope.
	@Override
	@GetMapping("/{issuancePublicId}/download")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ID_CARD_ISSUE')")
	public ResponseEntity<byte[]> download(@PathVariable String teacherPublicId,
			@PathVariable String issuancePublicId) {
		DocumentIssuance issuance = idCardService.findTeacherCard(teacherPublicId, issuancePublicId);
		byte[] pdf = idCardService.downloadPdf(issuance);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(issuancePublicId + ".pdf").build().toString())
				.body(pdf);
	}
}

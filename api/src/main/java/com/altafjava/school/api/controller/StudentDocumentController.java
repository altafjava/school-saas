package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import com.altafjava.school.api.controller.api.StudentDocumentApi;
import com.altafjava.school.api.dto.request.RejectDocumentRequest;
import com.altafjava.school.api.dto.request.UploadStudentDocumentRequest;
import com.altafjava.school.api.dto.response.StudentDocumentResponse;
import com.altafjava.school.api.mapper.StudentDocumentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.StudentDocumentService;

@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/documents")
public class StudentDocumentController implements StudentDocumentApi {

	private final StudentDocumentService studentDocumentService;
	private final StudentDocumentMapper studentDocumentMapper;
	private final SpringDataPageableResolver pageableResolver;

	public StudentDocumentController(StudentDocumentService studentDocumentService,
			StudentDocumentMapper studentDocumentMapper, SpringDataPageableResolver pageableResolver) {
		this.studentDocumentService = studentDocumentService;
		this.studentDocumentMapper = studentDocumentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_DOCUMENT_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentDocumentResponse>> list(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				studentDocumentService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(document -> studentDocumentMapper.toResponse(document, studentPublicId))));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_DOCUMENT_MANAGE')")
	public ApiResponse<StudentDocumentResponse> upload(@PathVariable String studentPublicId,
			@Valid @RequestBody UploadStudentDocumentRequest request) {
		var document = studentDocumentService.uploadForStudent(studentPublicId, request.documentType(),
				request.filePublicId());
		return ApiResponse.success(studentDocumentMapper.toResponse(document, studentPublicId));
	}

	@Override
	@PatchMapping("/{documentPublicId}/verify")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_DOCUMENT_MANAGE')")
	public ApiResponse<StudentDocumentResponse> verify(@PathVariable String studentPublicId,
			@PathVariable String documentPublicId, @AuthenticationPrincipal AuthenticatedUser user) {
		var document = studentDocumentService.verify(documentPublicId, user.getId());
		return ApiResponse.success(studentDocumentMapper.toResponse(document, studentPublicId));
	}

	@Override
	@PatchMapping("/{documentPublicId}/reject")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_DOCUMENT_MANAGE')")
	public ApiResponse<StudentDocumentResponse> reject(@PathVariable String studentPublicId,
			@PathVariable String documentPublicId, @Valid @RequestBody RejectDocumentRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		var document = studentDocumentService.reject(documentPublicId, user.getId(), request.rejectionReason());
		return ApiResponse.success(studentDocumentMapper.toResponse(document, studentPublicId));
	}
}

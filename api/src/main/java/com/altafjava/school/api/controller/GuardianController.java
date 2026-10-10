package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.altafjava.school.api.controller.api.GuardianApi;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateGuardianRequest;
import com.altafjava.school.api.dto.request.GrantGuardianConsentRequest;
import com.altafjava.school.api.dto.request.LinkGuardianRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.response.GuardianConsentRecordResponse;
import com.altafjava.school.api.dto.response.GuardianResponse;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.AddressMapper;
import com.altafjava.school.api.mapper.GuardianConsentRecordMapper;
import com.altafjava.school.api.mapper.GuardianMapper;
import com.altafjava.school.api.mapper.StudentGuardianLinkMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.GuardianConsentService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.domain.guardian.model.GuardianConsentType;

@RestController
@RequestMapping("/api/v1/guardians")
public class GuardianController implements GuardianApi {

	private final GuardianService guardianService;
	private final GuardianConsentService guardianConsentService;
	private final GuardianMapper guardianMapper;
	private final AddressMapper addressMapper;
	private final StudentGuardianLinkMapper studentGuardianLinkMapper;
	private final StudentMapper studentMapper;
	private final GuardianConsentRecordMapper guardianConsentRecordMapper;

	private final SpringDataPageableResolver pageableResolver;

	public GuardianController(GuardianService guardianService, GuardianConsentService guardianConsentService,
			GuardianMapper guardianMapper, AddressMapper addressMapper,
			StudentGuardianLinkMapper studentGuardianLinkMapper, StudentMapper studentMapper,
			GuardianConsentRecordMapper guardianConsentRecordMapper, SpringDataPageableResolver pageableResolver) {
		this.guardianService = guardianService;
		this.guardianConsentService = guardianConsentService;
		this.guardianMapper = guardianMapper;
		this.addressMapper = addressMapper;
		this.studentGuardianLinkMapper = studentGuardianLinkMapper;
		this.studentMapper = studentMapper;
		this.guardianConsentRecordMapper = guardianConsentRecordMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	@SortableBy({ "firstName", "lastName" })
	public ApiResponse<com.altafjava.platform.core.model.Page<GuardianResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(
				PlatformPageMapper
						.toPlatformPage(guardianService.searchGuardians(pageableResolver.resolve(page, size), q)
								.map(guardianMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<GuardianResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(guardianMapper.toResponse(guardianService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<GuardianResponse> create(@Valid @RequestBody CreateGuardianRequest request) {
		return ApiResponse.success(guardianMapper.toResponse(guardianService.create(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phone(),
				request.userPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/address")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<GuardianResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request) {
		return ApiResponse.success(
				guardianMapper.toResponse(guardianService.updateAddress(publicId, addressMapper.toDomain(request))));
	}

	@Override
	@PatchMapping("/{publicId}/phone")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<GuardianResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request) {
		return ApiResponse.success(guardianMapper.toResponse(guardianService.updatePhone(publicId, request.phone())));
	}

	@Override
	@PatchMapping("/{publicId}/photo")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<GuardianResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request) {
		return ApiResponse
				.success(guardianMapper.toResponse(guardianService.updatePhoto(publicId, request.filePublicId())));
	}

	@Override
	@PostMapping("/{publicId}/students")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> linkStudent(@PathVariable String publicId,
			@Valid @RequestBody LinkGuardianRequest request) {
		return ApiResponse.success(studentGuardianLinkMapper.toResponse(guardianService.linkToStudent(
				publicId,
				request.studentPublicId(),
				request.relationshipType(),
				request.primaryContact())));
	}

	@Override
	@PatchMapping("/{guardianPublicId}/students/{studentPublicId}/consent/grant")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_CONSENT_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> grantConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId) {
		return ApiResponse.success(studentGuardianLinkMapper.toResponse(
				guardianService.grantConsent(guardianPublicId, studentPublicId)));
	}

	@Override
	@PatchMapping("/{guardianPublicId}/students/{studentPublicId}/consent/revoke")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_CONSENT_MANAGE')")
	public ApiResponse<StudentGuardianLinkResponse> revokeConsent(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId) {
		return ApiResponse.success(studentGuardianLinkMapper.toResponse(
				guardianService.revokeConsent(guardianPublicId, studentPublicId)));
	}

	@Override
	@GetMapping("/me/students")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_SELF_SERVICE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> myStudents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(guardianService.listLinkedStudentsForCurrentUser(pageableResolver.resolve(page, size))
						.map(studentMapper::toResponse)));
	}

	@Override
	@PostMapping("/me/students/{studentPublicId}/consents")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_SELF_SERVICE')")
	public ApiResponse<GuardianConsentRecordResponse> grantSelfConsent(@PathVariable String studentPublicId,
			@Valid @RequestBody GrantGuardianConsentRequest request) {
		return ApiResponse.success(guardianConsentRecordMapper.toResponse(
				guardianConsentService.grant(studentPublicId, request.consentType(), request.policyVersion())));
	}

	@Override
	@DeleteMapping("/me/students/{studentPublicId}/consents/{consentType}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_SELF_SERVICE')")
	public ApiResponse<GuardianConsentRecordResponse> revokeSelfConsent(@PathVariable String studentPublicId,
			@PathVariable GuardianConsentType consentType) {
		return ApiResponse.success(
				guardianConsentRecordMapper.toResponse(guardianConsentService.revoke(studentPublicId, consentType)));
	}

	@Override
	@GetMapping("/me/students/{studentPublicId}/consents")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_SELF_SERVICE')")
	public ApiResponse<List<GuardianConsentRecordResponse>> myConsents(@PathVariable String studentPublicId) {
		return ApiResponse.success(guardianConsentService.listMine(studentPublicId).stream()
				.map(guardianConsentRecordMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/students/{studentPublicId}/consents")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('GUARDIAN_MANAGE')")
	public ApiResponse<List<GuardianConsentRecordResponse>> studentConsents(@PathVariable String studentPublicId) {
		return ApiResponse.success(guardianConsentService.listForStudent(studentPublicId).stream()
				.map(guardianConsentRecordMapper::toResponse).toList());
	}
}

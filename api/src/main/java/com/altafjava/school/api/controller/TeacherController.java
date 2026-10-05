package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.altafjava.school.api.controller.api.TeacherApi;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateTeacherRequest;
import com.altafjava.school.api.dto.request.SetTeacherProbationRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.request.UpdateTeacherContactDetailsRequest;
import com.altafjava.school.api.dto.request.UpdateTeacherHrDetailsRequest;
import com.altafjava.school.api.dto.response.TeacherResponse;
import com.altafjava.school.api.mapper.AddressMapper;
import com.altafjava.school.api.mapper.TeacherMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.TeacherService;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController implements TeacherApi {

	private final TeacherService teacherService;
	private final TeacherMapper teacherMapper;
	private final AddressMapper addressMapper;

	private final SpringDataPageableResolver pageableResolver;

	public TeacherController(TeacherService teacherService, TeacherMapper teacherMapper, AddressMapper addressMapper,
			SpringDataPageableResolver pageableResolver) {
		this.teacherService = teacherService;
		this.teacherMapper = teacherMapper;
		this.addressMapper = addressMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<TeacherResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(
				PlatformPageMapper.toPlatformPage(teacherService.listTeachers(pageableResolver.resolve(page, size))
						.map(teacherMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(teacherMapper.toResponse(teacherService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> hire(@Valid @RequestBody CreateTeacherRequest request) {
		return ApiResponse.success(teacherMapper.toResponse(teacherService.hire(
				request.employeeCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.joinDate())));
	}

	@Override
	@PatchMapping("/{publicId}/contact-details")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateTeacherContactDetailsRequest request) {
		return ApiResponse
				.success(teacherMapper.toResponse(teacherService.updateContactDetails(publicId, request.firstName(),
						request.lastName(), request.email())));
	}

	@Override
	@PatchMapping("/{publicId}/hr-details")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> updateHrDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateTeacherHrDetailsRequest request) {
		return ApiResponse
				.success(teacherMapper.toResponse(teacherService.updateHrDetails(publicId, request.departmentPublicId(),
						request.qualification(), request.employmentType())));
	}

	@Override
	@PatchMapping("/{publicId}/phone")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request) {
		return ApiResponse.success(teacherMapper.toResponse(teacherService.updatePhone(publicId, request.phone())));
	}

	@Override
	@PatchMapping("/{publicId}/address")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request) {
		return ApiResponse.success(
				teacherMapper.toResponse(teacherService.updateAddress(publicId, addressMapper.toDomain(request))));
	}

	@Override
	@PatchMapping("/{publicId}/photo")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request) {
		return ApiResponse
				.success(teacherMapper.toResponse(teacherService.updatePhoto(publicId, request.filePublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> setProbationPeriod(@PathVariable String publicId,
			@Valid @RequestBody SetTeacherProbationRequest request) {
		return ApiResponse.success(
				teacherMapper.toResponse(teacherService.setProbationPeriod(publicId, request.probationEndDate())));
	}

	@Override
	@PatchMapping("/{publicId}/probation/end")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TEACHER_MANAGE')")
	public ApiResponse<TeacherResponse> endProbation(@PathVariable String publicId) {
		return ApiResponse.success(teacherMapper.toResponse(teacherService.endProbation(publicId)));
	}
}

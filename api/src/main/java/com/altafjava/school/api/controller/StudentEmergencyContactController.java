package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.StudentEmergencyContactApi;
import com.altafjava.school.api.dto.request.EmergencyContactRequest;
import com.altafjava.school.api.dto.request.UpdateEmergencyContactRequest;
import com.altafjava.school.api.dto.response.EmergencyContactResponse;
import com.altafjava.school.api.mapper.EmergencyContactMapper;
import com.altafjava.school.application.service.EmergencyContactService;

@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/emergency-contacts")
public class StudentEmergencyContactController implements StudentEmergencyContactApi {

	private final EmergencyContactService emergencyContactService;
	private final EmergencyContactMapper emergencyContactMapper;

	public StudentEmergencyContactController(EmergencyContactService emergencyContactService,
			EmergencyContactMapper emergencyContactMapper) {
		this.emergencyContactService = emergencyContactService;
		this.emergencyContactMapper = emergencyContactMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMERGENCY_CONTACT_MANAGE')")
	public ApiResponse<List<EmergencyContactResponse>> list(@PathVariable String studentPublicId) {
		return ApiResponse.success(emergencyContactService.listForStudent(studentPublicId).stream()
				.map(emergencyContactMapper::toResponse).toList());
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMERGENCY_CONTACT_MANAGE')")
	public ApiResponse<EmergencyContactResponse> add(@PathVariable String studentPublicId,
			@Valid @RequestBody EmergencyContactRequest request) {
		return ApiResponse.success(emergencyContactMapper.toResponse(
				emergencyContactService.add(studentPublicId, request.name(), request.relationship(), request.phone(),
						request.alternatePhone(), request.priority())));
	}

	@Override
	@PutMapping("/{contactPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMERGENCY_CONTACT_MANAGE')")
	public ApiResponse<EmergencyContactResponse> update(@PathVariable String studentPublicId,
			@PathVariable String contactPublicId, @Valid @RequestBody UpdateEmergencyContactRequest request) {
		return ApiResponse.success(emergencyContactMapper.toResponse(
				emergencyContactService.update(studentPublicId, contactPublicId, request.name(),
						request.relationship(), request.phone(), request.alternatePhone(), request.priority(),
						request.expectedVersion())));
	}

	@Override
	@DeleteMapping("/{contactPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMERGENCY_CONTACT_MANAGE')")
	public ApiResponse<Void> remove(@PathVariable String studentPublicId, @PathVariable String contactPublicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		emergencyContactService.remove(studentPublicId, contactPublicId, user.getId());
		return ApiResponse.success(null);
	}
}

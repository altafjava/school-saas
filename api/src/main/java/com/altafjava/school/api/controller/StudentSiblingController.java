package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.StudentSiblingApi;
import com.altafjava.school.api.dto.request.LinkSiblingRequest;
import com.altafjava.school.api.dto.response.SiblingResponse;
import com.altafjava.school.api.mapper.SiblingMapper;
import com.altafjava.school.application.service.StudentSiblingService;

@RestController
@RequestMapping("/api/v1/students/{studentPublicId}/siblings")
public class StudentSiblingController implements StudentSiblingApi {

	private final StudentSiblingService studentSiblingService;
	private final SiblingMapper siblingMapper;

	public StudentSiblingController(StudentSiblingService studentSiblingService, SiblingMapper siblingMapper) {
		this.studentSiblingService = studentSiblingService;
		this.siblingMapper = siblingMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_READ')")
	public ApiResponse<List<SiblingResponse>> list(@PathVariable String studentPublicId) {
		return ApiResponse
				.success(siblingMapper.toResponseList(studentSiblingService.listSiblings(studentPublicId)));
	}

	@Override
	@GetMapping("/suggestions")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<List<SiblingResponse>> suggest(@PathVariable String studentPublicId) {
		return ApiResponse
				.success(siblingMapper.toResponseList(studentSiblingService.suggestSiblings(studentPublicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<List<SiblingResponse>> link(@PathVariable String studentPublicId,
			@Valid @RequestBody LinkSiblingRequest request) {
		return ApiResponse.success(
				siblingMapper.toResponseList(studentSiblingService.link(studentPublicId, request.siblingPublicId())));
	}

	@Override
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<Void> leave(@PathVariable String studentPublicId) {
		studentSiblingService.leave(studentPublicId);
		return ApiResponse.success(null);
	}
}

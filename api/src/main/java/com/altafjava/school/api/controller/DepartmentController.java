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
import com.altafjava.school.api.controller.api.DepartmentApi;
import com.altafjava.school.api.dto.request.AssignHeadEmployeeRequest;
import com.altafjava.school.api.dto.request.CreateDepartmentRequest;
import com.altafjava.school.api.dto.request.UpdateDepartmentRequest;
import com.altafjava.school.api.dto.response.DepartmentResponse;
import com.altafjava.school.api.mapper.DepartmentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController implements DepartmentApi {

	private final DepartmentService departmentService;
	private final DepartmentMapper departmentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public DepartmentController(DepartmentService departmentService, DepartmentMapper departmentMapper,
			SpringDataPageableResolver pageableResolver) {
		this.departmentService = departmentService;
		this.departmentMapper = departmentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<DepartmentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				departmentService.list(q, pageableResolver.resolve(page, size)).map(departmentMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<DepartmentResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(departmentMapper.toResponse(departmentService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
		return ApiResponse.success(departmentMapper
				.toResponse(departmentService.create(request.name(), request.code(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<DepartmentResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateDepartmentRequest request) {
		return ApiResponse.success(departmentMapper.toResponse(
				departmentService.updateDetails(publicId, request.name(), request.code(), request.description(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/head-employee")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<DepartmentResponse> assignHeadEmployee(@PathVariable String publicId,
			@Valid @RequestBody AssignHeadEmployeeRequest request) {
		return ApiResponse.success(departmentMapper
				.toResponse(departmentService.assignHeadEmployee(publicId, request.headEmployeePublicId(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/deactivate")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DEPARTMENT_MANAGE')")
	public ApiResponse<DepartmentResponse> deactivate(@PathVariable String publicId) {
		return ApiResponse.success(departmentMapper.toResponse(departmentService.deactivate(publicId)));
	}
}

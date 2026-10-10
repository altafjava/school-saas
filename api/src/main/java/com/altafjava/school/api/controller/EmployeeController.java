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
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.school.api.controller.api.EmployeeApi;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateEmployeeRequest;
import com.altafjava.school.api.dto.request.ExitEmployeeRequest;
import com.altafjava.school.api.dto.request.SetEmployeeProbationRequest;
import com.altafjava.school.api.dto.request.UpdateEmployeeContactDetailsRequest;
import com.altafjava.school.api.dto.request.UpdateEmployeeHrDetailsRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.response.EmployeeResponse;
import com.altafjava.school.api.mapper.AddressMapper;
import com.altafjava.school.api.mapper.EmployeeMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController implements EmployeeApi {

	private final EmployeeService employeeService;
	private final EmployeeMapper employeeMapper;
	private final AddressMapper addressMapper;
	private final SpringDataPageableResolver pageableResolver;

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_READ')")
	@SortableBy({ "employeeCode", "firstName", "lastName", "joinDate", "status", "staffCategory" })
	public ApiResponse<com.altafjava.platform.core.model.Page<EmployeeResponse>> list(
			@RequestParam(required = false) StaffCategory category,
			@RequestParam(required = false) EmployeeStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String departmentPublicId) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				employeeService.search(category, status, departmentPublicId, q, pageableResolver.resolve(page, size))
						.map(employeeMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_READ')")
	public ApiResponse<EmployeeResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(employeeMapper.toResponse(employeeService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> hire(@Valid @RequestBody CreateEmployeeRequest request) {
		return ApiResponse.success(employeeMapper.toResponse(employeeService.hire(request.staffCategory(),
				request.employeeCode(), request.firstName(), request.lastName(), request.email(),
				request.gender(), request.joinDate())));
	}

	@Override
	@PatchMapping("/{publicId}/contact-details")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEmployeeContactDetailsRequest request) {
		return ApiResponse.success(employeeMapper.toResponse(employeeService.updateContactDetails(publicId,
				request.firstName(), request.lastName(), request.email(), request.gender(),
				request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/hr-details")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> updateHrDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEmployeeHrDetailsRequest request) {
		return ApiResponse.success(employeeMapper.toResponse(employeeService.updateHrDetails(publicId,
				request.departmentPublicId(), request.designation(), request.qualification(),
				request.employmentType(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/phone")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request) {
		return ApiResponse.success(employeeMapper
				.toResponse(employeeService.updatePhone(publicId, request.phone(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/address")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request) {
		return ApiResponse.success(
				employeeMapper.toResponse(employeeService.updateAddress(publicId, addressMapper.toDomain(request),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/photo")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request) {
		return ApiResponse
				.success(employeeMapper.toResponse(
						employeeService.updatePhoto(publicId, request.filePublicId(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/probation")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> setProbationPeriod(@PathVariable String publicId,
			@Valid @RequestBody SetEmployeeProbationRequest request) {
		return ApiResponse.success(employeeMapper
				.toResponse(employeeService.setProbationPeriod(publicId, request.probationEndDate(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/probation/end")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> endProbation(@PathVariable String publicId) {
		return ApiResponse.success(employeeMapper.toResponse(employeeService.endProbation(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/exit")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('EMPLOYEE_MANAGE')")
	public ApiResponse<EmployeeResponse> exit(@PathVariable String publicId,
			@Valid @RequestBody ExitEmployeeRequest request) {
		return ApiResponse.success(employeeMapper
				.toResponse(employeeService.exit(publicId, request.status(), request.exitDate(), request.reason())));
	}
}

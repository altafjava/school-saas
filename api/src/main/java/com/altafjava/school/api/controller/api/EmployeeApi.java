package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateEmployeeRequest;
import com.altafjava.school.api.dto.request.ExitEmployeeRequest;
import com.altafjava.school.api.dto.request.SetEmployeeProbationRequest;
import com.altafjava.school.api.dto.request.UpdateEmployeeContactDetailsRequest;
import com.altafjava.school.api.dto.request.UpdateEmployeeHrDetailsRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.response.EmployeeResponse;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.model.StaffCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Employee", description = "HR records for all staff — teaching and non-teaching. A teacher's public id is also its employee public id, so every operation here works for teachers too.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface EmployeeApi {

	@Operation(summary = "List", operationId = "employee_list")
	ApiResponse<com.altafjava.platform.core.model.Page<EmployeeResponse>> list(
			@RequestParam(required = false) StaffCategory category,
			@RequestParam(required = false) EmployeeStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "employee_get")
	ApiResponse<EmployeeResponse> get(@PathVariable String publicId);

	@Operation(summary = "Hire a non-teaching employee", operationId = "employee_hire")
	ApiResponse<EmployeeResponse> hire(@Valid @RequestBody CreateEmployeeRequest request);

	@Operation(summary = "Update contact details", operationId = "employee_updateContactDetails")
	ApiResponse<EmployeeResponse> updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEmployeeContactDetailsRequest request);

	@Operation(summary = "Update HR details", operationId = "employee_updateHrDetails")
	ApiResponse<EmployeeResponse> updateHrDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateEmployeeHrDetailsRequest request);

	@Operation(summary = "Update phone", operationId = "employee_updatePhone")
	ApiResponse<EmployeeResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request);

	@Operation(summary = "Update address", operationId = "employee_updateAddress")
	ApiResponse<EmployeeResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request);

	@Operation(summary = "Update photo", operationId = "employee_updatePhoto")
	ApiResponse<EmployeeResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request);

	@Operation(summary = "Set probation period", operationId = "employee_setProbationPeriod")
	ApiResponse<EmployeeResponse> setProbationPeriod(@PathVariable String publicId,
			@Valid @RequestBody SetEmployeeProbationRequest request);

	@Operation(summary = "End probation", operationId = "employee_endProbation")
	ApiResponse<EmployeeResponse> endProbation(@PathVariable String publicId);

	@Operation(summary = "Record that the employee has left the school", operationId = "employee_exit")
	ApiResponse<EmployeeResponse> exit(@PathVariable String publicId, @Valid @RequestBody ExitEmployeeRequest request);
}

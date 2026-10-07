package com.altafjava.school.api.controller.api;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.GuardianAuthorizationChangeResponse;
import com.altafjava.school.api.dto.response.PickupAuthorizedGuardianResponse;
import com.altafjava.school.api.dto.response.PickupCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Pickup", description = "APIs for answering whether an adult may collect a student.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentPickupApi {

	@Operation(summary = "List guardians authorized to collect the student", operationId = "studentpickup_listAuthorized")
	ApiResponse<List<PickupAuthorizedGuardianResponse>> listAuthorized(@PathVariable String studentPublicId);

	@Operation(summary = "Check whether a guardian may collect the student", operationId = "studentpickup_check")
	ApiResponse<PickupCheckResponse> check(@PathVariable String studentPublicId,
			@RequestParam String guardianPublicId);

	@Operation(summary = "Pickup authorization and custody change history", operationId = "studentpickup_history")
	ApiResponse<List<GuardianAuthorizationChangeResponse>> history(@PathVariable String studentPublicId);
}

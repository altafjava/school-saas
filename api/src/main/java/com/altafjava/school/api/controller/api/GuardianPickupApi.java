package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.RestrictCustodyRequest;
import com.altafjava.school.api.dto.response.StudentGuardianLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Guardian Pickup", description = "APIs for authorizing who may collect a student and for restricting a guardian's custody. Every change is audited and kept in an append-only history.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface GuardianPickupApi {

	@Operation(summary = "Authorize pickup", operationId = "guardianpickup_authorize")
	ApiResponse<StudentGuardianLinkResponse> authorizePickup(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Revoke pickup authorization", operationId = "guardianpickup_revoke")
	ApiResponse<StudentGuardianLinkResponse> revokePickup(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Restrict custody", operationId = "guardianpickup_restrictCustody")
	ApiResponse<StudentGuardianLinkResponse> restrictCustody(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @Valid @RequestBody RestrictCustodyRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Lift custody restriction", operationId = "guardianpickup_liftCustodyRestriction")
	ApiResponse<StudentGuardianLinkResponse> liftCustodyRestriction(@PathVariable String guardianPublicId,
			@PathVariable String studentPublicId, @AuthenticationPrincipal AuthenticatedUser user);
}

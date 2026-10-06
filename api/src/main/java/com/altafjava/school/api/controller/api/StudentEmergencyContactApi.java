package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.EmergencyContactRequest;
import com.altafjava.school.api.dto.response.EmergencyContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Emergency Contacts", description = "APIs for managing who to call about a student in an emergency (need not be a guardian).\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentEmergencyContactApi {

	@Operation(summary = "List", operationId = "emergencycontact_list")
	ApiResponse<List<EmergencyContactResponse>> list(@PathVariable String studentPublicId);

	@Operation(summary = "Add", operationId = "emergencycontact_add")
	ApiResponse<EmergencyContactResponse> add(@PathVariable String studentPublicId,
			@Valid @RequestBody EmergencyContactRequest request);

	@Operation(summary = "Update", operationId = "emergencycontact_update")
	ApiResponse<EmergencyContactResponse> update(@PathVariable String studentPublicId,
			@PathVariable String contactPublicId, @Valid @RequestBody EmergencyContactRequest request);

	@Operation(summary = "Remove", operationId = "emergencycontact_remove")
	ApiResponse<Void> remove(@PathVariable String studentPublicId, @PathVariable String contactPublicId,
			@AuthenticationPrincipal AuthenticatedUser user);
}

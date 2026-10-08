package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.LinkSiblingRequest;
import com.altafjava.school.api.dto.response.SiblingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Sibling", description = "APIs for managing Student Sibling operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentSiblingApi {

	@Operation(summary = "List siblings", operationId = "studentsibling_list", description = "The student's siblings at the school, whatever their enrollment status.")
	public ApiResponse<List<SiblingResponse>> list(@PathVariable String studentPublicId);

	@Operation(summary = "Suggest siblings", operationId = "studentsibling_suggest", description = "Students sharing a guardian with this one who are not yet recorded as siblings.")
	public ApiResponse<List<SiblingResponse>> suggest(@PathVariable String studentPublicId);

	@Operation(summary = "Link sibling", operationId = "studentsibling_link", description = "Records the two students as siblings and returns all of the student's siblings. Siblinghood "
			+ "is transitive: a student who already has siblings brings them all into the family.")
	public ApiResponse<List<SiblingResponse>> link(@PathVariable String studentPublicId,
			@Valid @RequestBody LinkSiblingRequest request);

	@Operation(summary = "Leave sibling group", operationId = "studentsibling_leave", description = "Removes only this student from their siblings. A family left with one member is dissolved.")
	public ApiResponse<Void> leave(@PathVariable String studentPublicId);
}

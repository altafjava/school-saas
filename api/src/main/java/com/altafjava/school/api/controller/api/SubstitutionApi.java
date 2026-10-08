package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.AssignSubstituteRequest;
import com.altafjava.school.api.dto.request.CancelSubstitutionRequest;
import com.altafjava.school.api.dto.response.AvailableTeacherResponse;
import com.altafjava.school.api.dto.response.SubstitutionResponse;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Substitution", description = "APIs for managing Substitution operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface SubstitutionApi {

	@Operation(summary = "List on date", operationId = "substitution_listOn", description = "The substitutions standing on one date.")
	public ApiResponse<List<SubstitutionResponse>> listOn(@RequestParam LocalDate date);

	@Operation(summary = "List uncovered", operationId = "substitution_listUncovered", description = "Slots on the date whose regular teacher is on approved leave and that have "
			+ "no substitute yet.")
	public ApiResponse<List<TimetableEntryResponse>> listUncovered(@RequestParam LocalDate date);

	@Operation(summary = "List available teachers", operationId = "substitution_listAvailableTeachers", description = "Active teachers free to cover the slot on the date: not teaching, "
			+ "covering or on approved leave in that period.")
	public ApiResponse<List<AvailableTeacherResponse>> listAvailableTeachers(
			@RequestParam String timetableEntryPublicId, @RequestParam LocalDate date);

	@Operation(summary = "Assign", operationId = "substitution_assign", description = "Has a free teacher cover one regular slot on one date. The date must fall on the slot's "
			+ "weekday, must not be in the past or a holiday.")
	public ApiResponse<SubstitutionResponse> assign(@Valid @RequestBody AssignSubstituteRequest request,
			@AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Cancel", operationId = "substitution_cancel")
	public ApiResponse<SubstitutionResponse> cancel(@PathVariable String publicId,
			@Valid @RequestBody CancelSubstitutionRequest request);
}

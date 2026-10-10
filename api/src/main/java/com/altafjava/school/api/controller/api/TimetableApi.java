package com.altafjava.school.api.controller.api;

import java.time.DayOfWeek;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AssignTimetableVenueRequest;
import com.altafjava.school.api.dto.request.CreateTimetableEntryRequest;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Timetable", description = "APIs for managing Timetable operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface TimetableApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<TimetableEntryResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String teacherPublicId,
			@RequestParam(required = false) String subjectPublicId,
			@RequestParam(required = false) DayOfWeek dayOfWeek);

	@Operation(summary = "Get")
	public ApiResponse<TimetableEntryResponse> get(@PathVariable String publicId);

	@Operation(summary = "Schedule", description = "A slot cannot double-book its class, its teacher or — when venuePublicId is given — "
			+ "its venue in the same day and period.")
	public ApiResponse<TimetableEntryResponse> schedule(@Valid @RequestBody CreateTimetableEntryRequest request);

	@Operation(summary = "Assign venue", description = "Holds the slot in a venue; refused if another slot already uses that venue in the "
			+ "same day and period.")
	public ApiResponse<TimetableEntryResponse> assignVenue(@PathVariable String publicId,
			@Valid @RequestBody AssignTimetableVenueRequest request);
}

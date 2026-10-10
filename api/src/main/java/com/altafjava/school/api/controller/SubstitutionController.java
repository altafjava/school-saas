package com.altafjava.school.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.SubstitutionApi;
import com.altafjava.school.api.dto.request.AssignSubstituteRequest;
import com.altafjava.school.api.dto.request.CancelSubstitutionRequest;
import com.altafjava.school.api.dto.response.AvailableTeacherResponse;
import com.altafjava.school.api.dto.response.SubstitutionResponse;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.SubstitutionMapper;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.application.service.SubstitutionService;

@RestController
@RequestMapping("/api/v1/timetable-substitutions")
public class SubstitutionController implements SubstitutionApi {

	private final SubstitutionService substitutionService;
	private final SubstitutionMapper substitutionMapper;
	private final TimetableEntryMapper timetableEntryMapper;

	public SubstitutionController(SubstitutionService substitutionService, SubstitutionMapper substitutionMapper,
			TimetableEntryMapper timetableEntryMapper) {
		this.substitutionService = substitutionService;
		this.substitutionMapper = substitutionMapper;
		this.timetableEntryMapper = timetableEntryMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('TIMETABLE_READ')")
	public ApiResponse<List<SubstitutionResponse>> listOn(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.success(substitutionService.listOn(date).stream().map(substitutionMapper::toResponse)
				.toList());
	}

	@Override
	@GetMapping("/uncovered")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBSTITUTION_MANAGE')")
	public ApiResponse<List<TimetableEntryResponse>> listUncovered(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.success(timetableEntryMapper.toResponseList(substitutionService.uncoveredOn(date)));
	}

	@Override
	@GetMapping("/available-teachers")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBSTITUTION_MANAGE')")
	public ApiResponse<List<AvailableTeacherResponse>> listAvailableTeachers(
			@RequestParam String timetableEntryPublicId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.success(substitutionService.availableTeachers(timetableEntryPublicId, date).stream()
				.map(substitutionMapper::toAvailableTeacher).toList());
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBSTITUTION_MANAGE')")
	public ApiResponse<SubstitutionResponse> assign(@Valid @RequestBody AssignSubstituteRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(substitutionMapper.toResponse(substitutionService.assign(
				request.timetableEntryPublicId(), request.date(), request.substituteTeacherPublicId(),
				request.reason(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('SUBSTITUTION_MANAGE')")
	public ApiResponse<SubstitutionResponse> cancel(@PathVariable String publicId,
			@Valid @RequestBody CancelSubstitutionRequest request) {
		return ApiResponse.success(substitutionMapper.toResponse(substitutionService.cancel(publicId,
				request.reason())));
	}
}

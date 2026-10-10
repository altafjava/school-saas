package com.altafjava.school.api.controller;

import java.time.LocalDate;
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
import com.altafjava.school.api.controller.api.DisciplineIncidentApi;
import com.altafjava.school.api.dto.request.RecordDisciplineActionRequest;
import com.altafjava.school.api.dto.request.RecordDisciplineIncidentRequest;
import com.altafjava.school.api.dto.response.DisciplineIncidentResponse;
import com.altafjava.school.api.mapper.DisciplineIncidentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.DisciplineIncidentFilter;
import com.altafjava.school.application.service.DisciplineIncidentService;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;

@RestController
@RequestMapping("/api/v1/discipline-incidents")
public class DisciplineIncidentController implements DisciplineIncidentApi {

	private final DisciplineIncidentService disciplineIncidentService;
	private final DisciplineIncidentMapper disciplineIncidentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public DisciplineIncidentController(DisciplineIncidentService disciplineIncidentService,
			DisciplineIncidentMapper disciplineIncidentMapper, SpringDataPageableResolver pageableResolver) {
		this.disciplineIncidentService = disciplineIncidentService;
		this.disciplineIncidentMapper = disciplineIncidentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DISCIPLINE_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<DisciplineIncidentResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) IncidentSeverity severity,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(disciplineIncidentService
						.listAll(new DisciplineIncidentFilter(studentPublicId, severity, new DateWindow(from, to)),
								pageableResolver.resolve(page, size))
						.map(disciplineIncidentMapper::toResponse)));
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DISCIPLINE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<DisciplineIncidentResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				disciplineIncidentService.listForStudent(studentPublicId, pageableResolver.resolve(page, size))
						.map(disciplineIncidentMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DISCIPLINE_WRITE')")
	public ApiResponse<DisciplineIncidentResponse> record(@Valid @RequestBody RecordDisciplineIncidentRequest request) {
		return ApiResponse
				.success(disciplineIncidentMapper.toResponse(disciplineIncidentService.record(request.studentPublicId(),
						request.incidentDate(), request.severity(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}/action")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('DISCIPLINE_ACTION')")
	public ApiResponse<DisciplineIncidentResponse> recordAction(@PathVariable String publicId,
			@Valid @RequestBody RecordDisciplineActionRequest request) {
		return ApiResponse.success(disciplineIncidentMapper.toResponse(
				disciplineIncidentService.recordAction(publicId, request.actionTaken())));
	}
}

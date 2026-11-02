package com.altafjava.school.api.controller;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import com.altafjava.platform.application.security.annotation.RequiresPermission;
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
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.DisciplineIncidentService;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/discipline-incidents")
public class DisciplineIncidentController implements DisciplineIncidentApi {

	private final DisciplineIncidentService disciplineIncidentService;
	private final DisciplineIncidentMapper disciplineIncidentMapper;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public DisciplineIncidentController(DisciplineIncidentService disciplineIncidentService,
			DisciplineIncidentMapper disciplineIncidentMapper, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.referenceSummaries = referenceSummaries;
		this.disciplineIncidentService = disciplineIncidentService;
		this.disciplineIncidentMapper = disciplineIncidentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.DISCIPLINE_MANAGE)
	public ApiResponse<com.altafjava.platform.core.model.Page<DisciplineIncidentResponse>> listAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) IncidentSeverity severity,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(disciplineIncidentService
						.listAll(new DisciplineIncidentFilter(studentPublicId, severity, new DateWindow(from, to)),
								pageableResolver.resolve(page, size)),
						disciplineIncidentMapper::toResponse)));
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@RequiresPermission(SchoolPermissions.DISCIPLINE_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<DisciplineIncidentResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				disciplineIncidentService.listForStudent(studentPublicId, pageableResolver.resolve(page, size)),
				disciplineIncidentMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.DISCIPLINE_WRITE)
	public ApiResponse<DisciplineIncidentResponse> record(@Valid @RequestBody RecordDisciplineIncidentRequest request) {
		return ApiResponse
				.success(disciplineIncidentMapper.toResponse(disciplineIncidentService.record(request.studentPublicId(),
						request.incidentDate(), request.severity(), request.description())));
	}

	@Override
	@PatchMapping("/{publicId}/action")
	@Command
	@RequiresPermission(SchoolPermissions.DISCIPLINE_ACTION)
	public ApiResponse<DisciplineIncidentResponse> recordAction(@PathVariable String publicId,
			@Valid @RequestBody RecordDisciplineActionRequest request) {
		return ApiResponse.success(disciplineIncidentMapper.toResponse(
				disciplineIncidentService.recordAction(publicId, request.actionTaken())));
	}
}

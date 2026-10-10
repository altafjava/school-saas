package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.HealthRecordApi;
import com.altafjava.school.api.dto.request.UpsertHealthRecordRequest;
import com.altafjava.school.api.dto.response.HealthRecordCorrectionResponse;
import com.altafjava.school.api.dto.response.HealthRecordResponse;
import com.altafjava.school.api.mapper.HealthRecordCorrectionMapper;
import com.altafjava.school.api.mapper.HealthRecordMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.HealthRecordService;

/**
 * Gated to {@code Roles.HAS_TENANT_ADMIN} only — no separate school/health-staff role
 * ("Nurse"/"HealthStaff") exists in the seeded role catalog, and this module does not invent new
 * platform-level RBAC infrastructure to add one. Health data is more sensitive than ordinary
 * operational data (see {@code HealthRecord}/{@code MedicalIncident} {@code @Pii} fields), so
 * unlike Transport/Hostel this intentionally excludes {@code TEACHER} from read access. A dedicated
 * health-staff role is a follow-up.
 */
@RestController
@RequestMapping("/api/v1/health-records")
public class HealthRecordController implements HealthRecordApi {

	private final HealthRecordService healthRecordService;
	private final HealthRecordMapper healthRecordMapper;
	private final HealthRecordCorrectionMapper healthRecordCorrectionMapper;
	private final SpringDataPageableResolver pageableResolver;

	public HealthRecordController(HealthRecordService healthRecordService, HealthRecordMapper healthRecordMapper,
			HealthRecordCorrectionMapper healthRecordCorrectionMapper, SpringDataPageableResolver pageableResolver) {
		this.healthRecordService = healthRecordService;
		this.healthRecordMapper = healthRecordMapper;
		this.healthRecordCorrectionMapper = healthRecordCorrectionMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HEALTH_RECORD_MANAGE')")
	public ApiResponse<HealthRecordResponse> getByStudent(@PathVariable String studentPublicId) {
		return ApiResponse.success(healthRecordMapper.toResponse(healthRecordService.getByStudent(studentPublicId)));
	}

	@Override
	@PutMapping("/students/{studentPublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HEALTH_RECORD_MANAGE')")
	public ApiResponse<HealthRecordResponse> upsert(@PathVariable String studentPublicId,
			@Valid @RequestBody UpsertHealthRecordRequest request) {
		return ApiResponse
				.success(healthRecordMapper.toResponse(healthRecordService.upsert(studentPublicId, request.bloodGroup(),
						request.allergies(), request.conditions(), request.immunizations(),
						request.expectedVersion())));
	}

	@Override
	@GetMapping("/students/{studentPublicId}/corrections")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('HEALTH_RECORD_MANAGE')")
	public ApiResponse<com.altafjava.platform.core.model.Page<HealthRecordCorrectionResponse>> listCorrections(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				healthRecordService.listCorrections(studentPublicId, pageableResolver.resolve(page, size))
						.map(healthRecordCorrectionMapper::toResponse)));
	}
}

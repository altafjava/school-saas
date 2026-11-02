package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.school.api.controller.api.LifecycleApi;
import com.altafjava.school.api.dto.response.LifecycleTransitionResponse;
import com.altafjava.school.api.mapper.LifecycleTransitionMapper;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.LifecycleTimelineService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LifecycleController implements LifecycleApi {

	private final ReferenceSummaries referenceSummaries;

	private final LifecycleTimelineService lifecycleTimelineService;
	private final LifecycleTransitionMapper lifecycleTransitionMapper;

	// A student, their guardian and staff may read a student's timeline; the service applies the
	// same own-child guard as every other student-scoped read.
	@Override
	@GetMapping("/students/{studentPublicId}/lifecycle")
	@RequiresPermission({ SchoolPermissions.STUDENT_READ, SchoolPermissions.STUDENT_SELF_SERVICE_READ,
			SchoolPermissions.GUARDIAN_SELF_SERVICE })
	public ApiResponse<List<LifecycleTransitionResponse>> studentTimeline(@PathVariable String studentPublicId) {
		return ApiResponse.success(referenceSummaries.mapList(lifecycleTimelineService.forStudent(studentPublicId),
				lifecycleTransitionMapper::toResponse));
	}

	@Override
	@GetMapping("/admissions/{admissionPublicId}/lifecycle")
	@RequiresPermission(SchoolPermissions.ADMISSION_MANAGE)
	public ApiResponse<List<LifecycleTransitionResponse>> admissionTimeline(@PathVariable String admissionPublicId) {
		return ApiResponse.success(referenceSummaries.mapList(lifecycleTimelineService.forAdmission(admissionPublicId),
				lifecycleTransitionMapper::toResponse));
	}
}

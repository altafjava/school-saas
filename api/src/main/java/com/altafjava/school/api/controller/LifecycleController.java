package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.LifecycleApi;
import com.altafjava.school.api.dto.response.LifecycleTransitionResponse;
import com.altafjava.school.api.mapper.LifecycleTransitionMapper;
import com.altafjava.school.application.service.LifecycleTimelineService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LifecycleController implements LifecycleApi {

	private final LifecycleTimelineService lifecycleTimelineService;
	private final LifecycleTransitionMapper lifecycleTransitionMapper;

	// A student, their guardian and staff may read a student's timeline; the service applies the
	// same own-child guard as every other student-scoped read.
	@Override
	@GetMapping("/students/{studentPublicId}/lifecycle")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_READ') or @permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ') or @permissionAuthorizationService.hasPermission('GUARDIAN_SELF_SERVICE')")
	public ApiResponse<List<LifecycleTransitionResponse>> studentTimeline(@PathVariable String studentPublicId) {
		return ApiResponse.success(lifecycleTimelineService.forStudent(studentPublicId).stream()
				.map(lifecycleTransitionMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/admissions/{admissionPublicId}/lifecycle")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('ADMISSION_MANAGE')")
	public ApiResponse<List<LifecycleTransitionResponse>> admissionTimeline(@PathVariable String admissionPublicId) {
		return ApiResponse.success(lifecycleTimelineService.forAdmission(admissionPublicId).stream()
				.map(lifecycleTransitionMapper::toResponse).toList());
	}
}

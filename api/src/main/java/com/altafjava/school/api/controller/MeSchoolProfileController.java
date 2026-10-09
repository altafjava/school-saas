package com.altafjava.school.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.response.SchoolProfileResponse;
import com.altafjava.school.application.service.SchoolProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Me", description = "The caller's own school records.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/me/school-profile")
@RequiredArgsConstructor
public class MeSchoolProfileController {

	private final SchoolProfileService schoolProfileService;

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "School profile", description = "Which student, staff, teacher and guardian records the caller is, and the students linked to a guardian. Complements GET /api/v1/me.")
	public ResponseEntity<ApiResponse<SchoolProfileResponse>> get(Authentication authentication) {
		if (!(authentication.getPrincipal() instanceof AuthenticatedUser user) || user.getId() == null
				|| user.getId() < 0) {
			throw new AccessDeniedException("A signed-in user is required");
		}
		SchoolProfileService.SchoolProfile profile = schoolProfileService.forUser(user.getId());
		SchoolProfileResponse response = new SchoolProfileResponse(
				str(profile.studentId()), str(profile.employeeId()), profile.teacher(), str(profile.guardianId()),
				profile.linkedStudents().stream().map(s -> new SchoolProfileResponse.LinkedStudent(
						s.publicId().toString(), s.name(), s.studentCode())).toList());
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(response));
	}

	private static String str(java.util.UUID id) {
		return id == null ? null : id.toString();
	}
}

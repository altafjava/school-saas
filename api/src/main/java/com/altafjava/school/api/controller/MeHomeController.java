package com.altafjava.school.api.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.school.api.dto.response.home.ParentHomeResponse;
import com.altafjava.school.api.dto.response.home.StudentHomeResponse;
import com.altafjava.school.api.dto.response.home.TeacherHomeResponse;
import com.altafjava.school.api.mapper.HomeMapper;
import com.altafjava.school.application.home.FamilyHomeService;
import com.altafjava.school.application.home.TeacherHomeService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Me", description = "The caller's own school records.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/me/home")
@RequiredArgsConstructor
public class MeHomeController {

	private final TeacherHomeService teacherHomeService;
	private final FamilyHomeService familyHomeService;
	private final HomeMapper homeMapper;

	@GetMapping("/teacher")
	@RequiresPermission(SchoolPermissions.TEACHER_HOME_READ)
	@Operation(summary = "Teacher home", description = "Today's timetable and substitutions, registers to mark, assignments to grade, exams needing marks, leave to review and the inbox, in one response. Requires a linked teacher record.")
	public ResponseEntity<ApiResponse<TeacherHomeResponse>> teacher() {
		return ok(homeMapper.toResponse(teacherHomeService.home()));
	}

	@GetMapping("/parent")
	@RequiresPermission(SchoolPermissions.PARENT_HOME_READ)
	@Operation(summary = "Parent home", description = "One summary per linked child, and the inbox, in one response. A custody-restricted child is left out. Requires a linked guardian record.")
	public ResponseEntity<ApiResponse<ParentHomeResponse>> parent() {
		return ok(homeMapper.toResponse(familyHomeService.parent()));
	}

	@GetMapping("/student")
	@RequiresPermission(SchoolPermissions.STUDENT_HOME_READ)
	@Operation(summary = "Student home", description = "Today's timetable, the student's own summary and the inbox, in one response. Requires a linked student record.")
	public ResponseEntity<ApiResponse<StudentHomeResponse>> student() {
		return ok(homeMapper.toResponse(familyHomeService.student()));
	}

	private static <T> ResponseEntity<ApiResponse<T>> ok(T home) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(home));
	}
}

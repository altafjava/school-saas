package com.altafjava.school.api.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.school.api.dto.response.ExamResponse;
import com.altafjava.school.api.mapper.ExamMapper;
import com.altafjava.school.application.exam.ExamTimetableService;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Exam timetable", description = "A classroom's exam dates, and their publication to its families.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/classrooms/{classroomPublicId}/exam-timetable")
@RequiredArgsConstructor
public class ClassroomExamTimetableController {

	private final ExamTimetableService examTimetableService;
	private final ExamMapper examMapper;
	private final ReferenceSummaries referenceSummaries;

	@GetMapping
	@RequiresPermission(SchoolPermissions.EXAM_READ)
	@Operation(summary = "Exam timetable", description = "The classroom's live exams in date order. Students and guardians "
			+ "see only published dates. The term narrows the list when given.")
	public ApiResponse<List<ExamResponse>> timetable(@PathVariable String classroomPublicId,
			@RequestParam(required = false) String termPublicId) {
		return ApiResponse.success(referenceSummaries
				.mapList(examTimetableService.timetable(classroomPublicId, termPublicId), examMapper::toResponse));
	}

	@PostMapping("/publish")
	@RequiresPermission(SchoolPermissions.EXAM_WRITE)
	@Operation(summary = "Publish exam timetable", description = "Makes every scheduled, unpublished exam of the classroom "
			+ "visible to its students and guardians, and tells them. The term narrows it when given.")
	public ApiResponse<List<ExamResponse>> publish(@PathVariable String classroomPublicId,
			@RequestParam(required = false) String termPublicId) {
		return ApiResponse.success(referenceSummaries
				.mapList(examTimetableService.publish(classroomPublicId, termPublicId), examMapper::toResponse));
	}
}

package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.StudentPromotionApi;
import com.altafjava.school.api.dto.request.PromoteStudentsRequest;
import com.altafjava.school.api.dto.response.PromotionPreviewResponse;
import com.altafjava.school.api.dto.response.PromotionResultResponse;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.StudentPromotionService;
import com.altafjava.school.application.service.StudentPromotionService.Decision;
import com.altafjava.school.application.service.StudentPromotionService.Preview;
import com.altafjava.school.application.service.StudentPromotionService.ResultRow;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/classrooms/{classroomPublicId}/promotions")
@RequiredArgsConstructor
public class StudentPromotionController implements StudentPromotionApi {

	private final StudentPromotionService studentPromotionService;
	private final ReferenceSummaries referenceSummaries;

	@Override
	@GetMapping("/preview")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_PROMOTE')")
	public ApiResponse<PromotionPreviewResponse> preview(@PathVariable String classroomPublicId,
			@RequestParam String targetClassroomPublicId) {
		Preview preview = studentPromotionService.preview(classroomPublicId, targetClassroomPublicId);
		List<PromotionPreviewResponse.PromotionPreviewRow> students = referenceSummaries.mapList(preview.rows(),
				row -> new PromotionPreviewResponse.PromotionPreviewRow(row.studentPublicId(),
						referenceSummaries.of(EntityRef.STUDENT, row.studentId()), row.enrollmentStatus(),
						row.alreadyPlaced(), row.blockedReason()));
		return ApiResponse.success(new PromotionPreviewResponse(classroomPublicId,
				referenceSummaries.of(EntityRef.CLASSROOM, preview.sourceClassroomId()), targetClassroomPublicId,
				referenceSummaries.of(EntityRef.CLASSROOM, preview.targetClassroomId()), preview.seatsLeft(),
				students));
	}

	@Override
	@PostMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_PROMOTE')")
	@RequireIdempotencyKey
	public ApiResponse<List<PromotionResultResponse>> promote(@PathVariable String classroomPublicId,
			@Valid @RequestBody PromoteStudentsRequest request) {
		List<Decision> decisions = request.entries().stream()
				.map(entry -> new Decision(entry.studentPublicId(), entry.outcome(), entry.targetClassroomPublicId(),
						entry.reason()))
				.toList();
		return ApiResponse.success(referenceSummaries.mapList(
				studentPromotionService.promote(classroomPublicId, request.targetClassroomPublicId(), decisions),
				this::toResponse));
	}

	private PromotionResultResponse toResponse(ResultRow row) {
		return new PromotionResultResponse(row.studentPublicId(),
				referenceSummaries.of(EntityRef.STUDENT, row.studentId()), row.outcome(), row.classroomPublicId(),
				referenceSummaries.of(EntityRef.CLASSROOM, row.classroomId()));
	}
}

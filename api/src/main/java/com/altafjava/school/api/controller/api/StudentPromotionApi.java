package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.PromoteStudentsRequest;
import com.altafjava.school.api.dto.response.PromotionPreviewResponse;
import com.altafjava.school.api.dto.response.PromotionResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student Promotion", description = "Moves a classroom into the next academic year.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentPromotionApi {

	@Operation(summary = "Preview", description = "Lists the roster and why any student cannot move. Changes nothing.")
	public ApiResponse<PromotionPreviewResponse> preview(@PathVariable String classroomPublicId,
			@RequestParam String targetClassroomPublicId);

	@Operation(summary = "Promote", description = "Applies each student's outcome as one step: if any fails, none is applied. A retained student needs their own target classroom.")
	public ApiResponse<List<PromotionResultResponse>> promote(@PathVariable String classroomPublicId,
			@Valid @RequestBody PromoteStudentsRequest request);
}

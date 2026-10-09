package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.PostLessonRequest;
import com.altafjava.school.api.dto.response.LessonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Lesson", description = "APIs for managing Lesson operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LessonApi {

	@Operation(summary = "Post")
	public ApiResponse<LessonResponse> post(@Valid @RequestBody PostLessonRequest request);

	@Operation(summary = "List by classroom")
	public ApiResponse<com.altafjava.platform.core.model.Page<LessonResponse>> listByClassroom(
			@PathVariable String classroomPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);
}

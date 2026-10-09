package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateExamTypeDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdateExamTypeDefinitionRequest;
import com.altafjava.school.api.dto.response.ExamTypeDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Exam Type Definition", description = "APIs for managing Exam Type Definition operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface ExamTypeDefinitionApi {

	@Operation(summary = "List", description = "Lists this tenant's exam type catalog (e.g. Unit Test, Midterm, Final), including inactive entries.")
	public ApiResponse<List<ExamTypeDefinitionResponse>> list();

	@Operation(summary = "List active", description = "Lists only active exam types — the set an exam-scheduling UI should offer.")
	public ApiResponse<List<ExamTypeDefinitionResponse>> listActive();

	@Operation(summary = "Get")
	public ApiResponse<ExamTypeDefinitionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", description = "Adds a new exam type to this tenant's catalog. Exam types are tenant data, not a fixed enum, "
			+ "so each tenant can define its own set (board/curriculum-specific terminology, extra exam categories).")
	public ApiResponse<ExamTypeDefinitionResponse> create(@Valid @RequestBody CreateExamTypeDefinitionRequest request);

	@Operation(summary = "Update", description = "Renames, reorders, or deactivates an exam type. Deactivating hides it from future exam "
			+ "scheduling without affecting exams already scheduled against it.")
	public ApiResponse<ExamTypeDefinitionResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdateExamTypeDefinitionRequest request);
}

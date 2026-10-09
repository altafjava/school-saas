package com.altafjava.school.api.controller.api;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.LifecycleTransitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Lifecycle", description = "Read-only, append-only history of a person's journey: application, enrollment, status changes, alumni.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LifecycleApi {

	@Operation(summary = "Student timeline, including the admission it came from")
	ApiResponse<List<LifecycleTransitionResponse>> studentTimeline(@PathVariable String studentPublicId);

	@Operation(summary = "Admission timeline")
	ApiResponse<List<LifecycleTransitionResponse>> admissionTimeline(@PathVariable String admissionPublicId);
}

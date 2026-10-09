package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.OrganizationRollupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Organization Rollup", description = "Cross-campus aggregate reporting for a multi-campus organization.\n\n**Scope**: SUPER_ADMIN or ORG_ADMIN only — this is org-wide, not a single tenant's data.\n**Auth**: JWT Bearer token required.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface OrganizationRollupApi {

	@Operation(summary = "Get", description = "Aggregates enrollment, attendance, and fee-collection totals across every campus (tenant) "
			+ "belonging to the organization for the given date range.")
	public ApiResponse<OrganizationRollupResponse> get(
			@PathVariable String organizationPublicId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to);
}

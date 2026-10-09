package com.altafjava.school.api.controller.api;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.response.LeaveBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Leave Balance", description = "APIs for managing Leave Balance operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface LeaveBalanceApi {

	@Operation(summary = "For employee")
	public ApiResponse<List<LeaveBalanceResponse>> forEmployee(@PathVariable String publicId,
			@RequestParam String academicYearPublicId);

	@Operation(summary = "For current employee")
	public ApiResponse<List<LeaveBalanceResponse>> forCurrentEmployee(@RequestParam String academicYearPublicId);
}

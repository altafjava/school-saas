package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreateBoardRequest;
import com.altafjava.school.api.dto.request.UpdateBoardRequest;
import com.altafjava.school.api.dto.response.BoardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Board", description = "APIs for managing Board operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface BoardApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<BoardResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<BoardResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<BoardResponse> create(@Valid @RequestBody CreateBoardRequest request);

	@Operation(summary = "Update details")
	public ApiResponse<BoardResponse> updateDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateBoardRequest request);

	@Operation(summary = "Deactivate")
	public ApiResponse<BoardResponse> deactivate(@PathVariable String publicId);
}

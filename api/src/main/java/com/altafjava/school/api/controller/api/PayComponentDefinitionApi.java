package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CreatePayComponentDefinitionRequest;
import com.altafjava.school.api.dto.request.UpdatePayComponentDefinitionRequest;
import com.altafjava.school.api.dto.response.PayComponentDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Pay Component Definition", description = "APIs for managing Pay Component Definition operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface PayComponentDefinitionApi {

	@Operation(summary = "List")
	public ApiResponse<List<PayComponentDefinitionResponse>> list();

	@Operation(summary = "List active")
	public ApiResponse<List<PayComponentDefinitionResponse>> listActive();

	@Operation(summary = "Get")
	public ApiResponse<PayComponentDefinitionResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create")
	public ApiResponse<PayComponentDefinitionResponse> create(
			@Valid @RequestBody CreatePayComponentDefinitionRequest request);

	@Operation(summary = "Update")
	public ApiResponse<PayComponentDefinitionResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdatePayComponentDefinitionRequest request);
}

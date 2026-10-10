package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.CheckoutBookRequest;
import com.altafjava.school.api.dto.request.ReturnBookRequest;
import com.altafjava.school.api.dto.response.CirculationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Circulation", description = "APIs for managing Circulation operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface CirculationApi {

	@Operation(summary = "List for student")
	public ApiResponse<com.altafjava.platform.core.model.Page<CirculationResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) String bookPublicId,
			@RequestParam(required = false) Boolean returned,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to);

	@Operation(summary = "Checkout", description = "A copy held for a reservation can only be checked out by the member it is held for.")
	public ApiResponse<CirculationResponse> checkout(@Valid @RequestBody CheckoutBookRequest request);

	@Operation(summary = "Return book")
	public ApiResponse<CirculationResponse> returnBook(@PathVariable String publicId,
			@Valid @RequestBody ReturnBookRequest request);

	@Operation(summary = "Renew", description = "Extends the due date by another loan period. Refused when the loan is overdue, has used "
			+ "its renewals (library.renewal.max-count, default 2), or another member is queuing for the title.")
	public ApiResponse<CirculationResponse> renew(@PathVariable String publicId);
}

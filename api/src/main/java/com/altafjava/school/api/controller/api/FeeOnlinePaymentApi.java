package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ConfirmFeeChargeRequest;
import com.altafjava.school.api.dto.request.CreateFeeChargeRequest;
import com.altafjava.school.api.dto.response.FeeChargeResponse;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Online Payment", description = "APIs for managing Fee Online Payment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface FeeOnlinePaymentApi {

	@Operation(summary = "Create charge", description = "Starts an online payment for a student's fee structure, sized to the current outstanding "
			+ "balance. Fails if there is nothing left to pay on that fee structure.")
	public ApiResponse<FeeChargeResponse> createCharge(@Valid @RequestBody CreateFeeChargeRequest request);

	@Operation(summary = "Confirm charge", description = "Confirms a gateway charge and records the payment. Idempotent on "
			+ "gatewayChargeReference — safe to retry if the client is unsure whether the first call landed.")
	public ApiResponse<FeePaymentResponse> confirmCharge(@PathVariable String gatewayChargeReference,
			@Valid @RequestBody ConfirmFeeChargeRequest request);

	@Operation(summary = "Get receipt")
	public ApiResponse<FeePaymentResponse> getReceipt(@PathVariable String publicId);
}

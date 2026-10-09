package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.GrantFeeDiscountRequest;
import com.altafjava.school.api.dto.request.RefundFeePaymentRequest;
import com.altafjava.school.api.dto.request.RevokeFeeDiscountRequest;
import com.altafjava.school.api.dto.request.SetFeeInstallmentPlanRequest;
import com.altafjava.school.api.dto.response.FeeDiscountResponse;
import com.altafjava.school.api.dto.response.FeeInstallmentResponse;
import com.altafjava.school.api.dto.response.FeeRefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Adjustments", description = "Discounts, installment plans and refunds applied on top of fee structures.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface FeeAdjustmentApi {

	@Operation(summary = "List a student's fee discounts")
	ApiResponse<List<FeeDiscountResponse>> listDiscounts(@PathVariable String studentPublicId);

	@Operation(summary = "Grant a fee discount")
	ApiResponse<FeeDiscountResponse> grantDiscount(@PathVariable String studentPublicId,
			@Valid @RequestBody GrantFeeDiscountRequest request, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Revoke a fee discount")
	ApiResponse<FeeDiscountResponse> revokeDiscount(@PathVariable String studentPublicId,
			@PathVariable String discountPublicId, @Valid @RequestBody RevokeFeeDiscountRequest request);

	@Operation(summary = "Get a student's installment plan for a fee structure")
	ApiResponse<List<FeeInstallmentResponse>> getPlan(@PathVariable String studentPublicId,
			@PathVariable String feeStructurePublicId);

	@Operation(summary = "Set (replace) a student's installment plan")
	ApiResponse<List<FeeInstallmentResponse>> setPlan(@PathVariable String studentPublicId,
			@PathVariable String feeStructurePublicId, @Valid @RequestBody SetFeeInstallmentPlanRequest request);

	@Operation(summary = "Remove a student's installment plan")
	ApiResponse<Void> removePlan(@PathVariable String studentPublicId, @PathVariable String feeStructurePublicId);

	@Operation(summary = "Refund (part of) a fee payment")
	ApiResponse<FeeRefundResponse> refund(@PathVariable String paymentPublicId,
			@Valid @RequestBody RefundFeePaymentRequest request, @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "List refunds of a fee payment")
	ApiResponse<List<FeeRefundResponse>> listRefundsForPayment(@PathVariable String paymentPublicId);

	@Operation(summary = "List a student's refunds")
	ApiResponse<List<FeeRefundResponse>> listRefundsForStudent(@PathVariable String studentPublicId);
}

package com.altafjava.school.api.controller;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.FeeAdjustmentApi;
import com.altafjava.school.api.dto.request.GrantFeeDiscountRequest;
import com.altafjava.school.api.dto.request.RefundFeePaymentRequest;
import com.altafjava.school.api.dto.request.RevokeFeeDiscountRequest;
import com.altafjava.school.api.dto.request.SetFeeInstallmentPlanRequest;
import com.altafjava.school.api.dto.response.FeeDiscountResponse;
import com.altafjava.school.api.dto.response.FeeInstallmentResponse;
import com.altafjava.school.api.dto.response.FeeRefundResponse;
import com.altafjava.school.api.mapper.FeeDiscountMapper;
import com.altafjava.school.api.mapper.FeeInstallmentMapper;
import com.altafjava.school.api.mapper.FeeRefundMapper;
import com.altafjava.school.api.ratelimit.RateLimited;
import com.altafjava.school.application.service.FeeDiscountService;
import com.altafjava.school.application.service.FeeInstallmentService;
import com.altafjava.school.application.service.FeeRefundService;
import com.altafjava.school.domain.fee.service.InstallmentPlanFactory.Slot;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeeAdjustmentController implements FeeAdjustmentApi {

	private static final BigDecimal BASIS_POINTS_PER_PERCENT = BigDecimal.valueOf(100);

	private final FeeDiscountService feeDiscountService;
	private final FeeInstallmentService feeInstallmentService;
	private final FeeRefundService feeRefundService;
	private final FeeDiscountMapper feeDiscountMapper;
	private final FeeInstallmentMapper feeInstallmentMapper;
	private final FeeRefundMapper feeRefundMapper;

	@Override
	@GetMapping("/students/{studentPublicId}/fee-discounts")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_DISCOUNT_MANAGE')")
	public ApiResponse<List<FeeDiscountResponse>> listDiscounts(@PathVariable String studentPublicId) {
		return ApiResponse.success(feeDiscountService.listForStudent(studentPublicId).stream()
				.map(feeDiscountMapper::toResponse).toList());
	}

	@Override
	@PostMapping("/students/{studentPublicId}/fee-discounts")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_DISCOUNT_MANAGE')")
	public ApiResponse<FeeDiscountResponse> grantDiscount(@PathVariable String studentPublicId,
			@Valid @RequestBody GrantFeeDiscountRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(feeDiscountMapper.toResponse(feeDiscountService.grant(studentPublicId,
				request.feeStructurePublicId(), request.type(), request.value(), request.category(),
				request.reason(), user.getId())));
	}

	@Override
	@PatchMapping("/students/{studentPublicId}/fee-discounts/{discountPublicId}/revoke")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_DISCOUNT_MANAGE')")
	public ApiResponse<FeeDiscountResponse> revokeDiscount(@PathVariable String studentPublicId,
			@PathVariable String discountPublicId, @Valid @RequestBody RevokeFeeDiscountRequest request) {
		return ApiResponse.success(feeDiscountMapper
				.toResponse(feeDiscountService.revoke(studentPublicId, discountPublicId, request.reason())));
	}

	@Override
	@GetMapping("/students/{studentPublicId}/fee-installment-plans/{feeStructurePublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_INSTALLMENT_MANAGE')")
	public ApiResponse<List<FeeInstallmentResponse>> getPlan(@PathVariable String studentPublicId,
			@PathVariable String feeStructurePublicId) {
		return ApiResponse.success(feeInstallmentService.getPlan(studentPublicId, feeStructurePublicId).stream()
				.map(feeInstallmentMapper::toResponse).toList());
	}

	@Override
	@PutMapping("/students/{studentPublicId}/fee-installment-plans/{feeStructurePublicId}")
	@LastWriteWins("Replaces the whole installment plan, which is a set of rows and not one versioned record")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_INSTALLMENT_MANAGE')")
	public ApiResponse<List<FeeInstallmentResponse>> setPlan(@PathVariable String studentPublicId,
			@PathVariable String feeStructurePublicId, @Valid @RequestBody SetFeeInstallmentPlanRequest request) {
		var plan = request.schedule() != null && !request.schedule().isEmpty()
				? feeInstallmentService.setCustomPlan(studentPublicId, feeStructurePublicId,
						request.schedule().stream()
								.map(slot -> new Slot(slot.dueDate(), toBasisPoints(slot.percentage())))
								.toList())
				: feeInstallmentService.setEvenPlan(studentPublicId, feeStructurePublicId, request.count(),
						request.firstDueDate(), request.intervalMonths() != null ? request.intervalMonths() : 1);
		return ApiResponse.success(plan.stream().map(feeInstallmentMapper::toResponse).toList());
	}

	@Override
	@DeleteMapping("/students/{studentPublicId}/fee-installment-plans/{feeStructurePublicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_INSTALLMENT_MANAGE')")
	public ApiResponse<Void> removePlan(@PathVariable String studentPublicId,
			@PathVariable String feeStructurePublicId) {
		feeInstallmentService.removePlan(studentPublicId, feeStructurePublicId);
		return ApiResponse.success(null);
	}

	@Override
	@PostMapping("/fee-payments/{paymentPublicId}/refunds")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_REFUND_MANAGE')")
	@RateLimited(key = "fee-refund", capacity = 30, periodMinutes = 60)
	public ApiResponse<FeeRefundResponse> refund(@PathVariable String paymentPublicId,
			@Valid @RequestBody RefundFeePaymentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(feeRefundMapper.toResponse(
				feeRefundService.refund(paymentPublicId, request.amount(), request.reason(), user.getId())));
	}

	@Override
	@GetMapping("/fee-payments/{paymentPublicId}/refunds")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_REFUND_MANAGE')")
	public ApiResponse<List<FeeRefundResponse>> listRefundsForPayment(@PathVariable String paymentPublicId) {
		return ApiResponse.success(feeRefundService.listForPayment(paymentPublicId).stream()
				.map(feeRefundMapper::toResponse).toList());
	}

	@Override
	@GetMapping("/students/{studentPublicId}/fee-refunds")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('FEE_REFUND_MANAGE')")
	public ApiResponse<List<FeeRefundResponse>> listRefundsForStudent(@PathVariable String studentPublicId) {
		return ApiResponse.success(feeRefundService.listForStudent(studentPublicId).stream()
				.map(feeRefundMapper::toResponse).toList());
	}

	private static int toBasisPoints(BigDecimal percentage) {
		return percentage.multiply(BASIS_POINTS_PER_PERCENT).intValueExact();
	}
}

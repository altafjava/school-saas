package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.platform.domain.paymentgateway.service.PaymentChargeResult;
import com.altafjava.school.api.controller.api.FeeOnlinePaymentApi;
import com.altafjava.school.api.dto.request.ConfirmFeeChargeRequest;
import com.altafjava.school.api.dto.request.CreateFeeChargeRequest;
import com.altafjava.school.api.dto.response.FeeChargeResponse;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import com.altafjava.school.api.mapper.FeePaymentMapper;
import com.altafjava.school.application.service.FeeOnlinePaymentService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

// Parent/student self-service fee payment: create a gateway charge, confirm it synchronously, and
// read back the resulting receipt. Deliberately separate from FeePaymentController (TENANT_ADMIN's
// manual-recording endpoints) rather than folding self-service in — different callers, different
// authorization model, and this controller never accepts a caller-supplied paidAmount/receiptNumber.
@RestController
@RequestMapping("/api/v1/fee-payments/self-service")
public class FeeOnlinePaymentController implements FeeOnlinePaymentApi {

	private final FeeOnlinePaymentService feeOnlinePaymentService;
	private final FeePaymentMapper feePaymentMapper;

	public FeeOnlinePaymentController(FeeOnlinePaymentService feeOnlinePaymentService,
			FeePaymentMapper feePaymentMapper) {
		this.feeOnlinePaymentService = feeOnlinePaymentService;
		this.feePaymentMapper = feePaymentMapper;
	}

	@Override
	@PostMapping("/charges")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_SELF_SERVICE)
	@RequireIdempotencyKey
	public ApiResponse<FeeChargeResponse> createCharge(@Valid @RequestBody CreateFeeChargeRequest request) {
		PaymentChargeResult result = feeOnlinePaymentService.createCharge(request.studentPublicId(),
				request.feeStructurePublicId());
		return ApiResponse.success(
				new FeeChargeResponse(result.gatewayChargeReference(), result.status(), result.clientSecret()));
	}

	@Override
	@PostMapping("/charges/{gatewayChargeReference}/confirm")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_SELF_SERVICE)
	@RequireIdempotencyKey
	public ApiResponse<FeePaymentResponse> confirmCharge(@PathVariable String gatewayChargeReference,
			@Valid @RequestBody ConfirmFeeChargeRequest request) {
		return ApiResponse
				.success(feePaymentMapper.toResponse(feeOnlinePaymentService.confirmCharge(request.studentPublicId(),
						request.feeStructurePublicId(), gatewayChargeReference)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_SELF_SERVICE)
	public ApiResponse<FeePaymentResponse> getReceipt(@PathVariable String publicId) {
		return ApiResponse
				.success(feePaymentMapper.toResponse(feeOnlinePaymentService.findReceiptForSelfService(publicId)));
	}
}

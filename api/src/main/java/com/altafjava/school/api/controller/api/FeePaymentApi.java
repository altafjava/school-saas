package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.BounceFeePaymentRequest;
import com.altafjava.school.api.dto.request.RecordFeePaymentRequest;
import com.altafjava.school.api.dto.response.FeeCollectionRowResponse;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import com.altafjava.school.domain.fee.model.PaymentMode;
import com.altafjava.school.domain.fee.model.PaymentSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fee Payment", description = "APIs for managing Fee Payment operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface FeePaymentApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeePaymentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) String feeStructurePublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) PaymentSource paymentSource,
			@RequestParam(required = false) PaymentMode paymentMode);

	@Operation(summary = "Get")
	public ApiResponse<FeePaymentResponse> get(@PathVariable String publicId);

	@Operation(summary = "Record")
	public ApiResponse<FeePaymentResponse> record(@Valid @RequestBody RecordFeePaymentRequest request);

	@Operation(summary = "Collection report", description = "Payments per day and mode for matching against cash and bank, up to 92 days. Bounced cheques are shown separately and never count as collected.")
	public ApiResponse<List<FeeCollectionRowResponse>> collectionReport(@RequestParam LocalDate from,
			@RequestParam LocalDate to);

	@Operation(summary = "Mark a cheque bounced", description = "Keeps the payment as evidence and restores the student's balance. A payment with refunds cannot bounce.")
	public ApiResponse<FeePaymentResponse> bounce(@PathVariable String publicId,
			@Valid @RequestBody BounceFeePaymentRequest request);

	@Operation(summary = "Download receipt", description = "The payment's receipt as a PDF with a verification code. Issued on the first download and the same document after that. A bounced cheque has no receipt, and bouncing revokes one already issued.")
	public ResponseEntity<byte[]> receipt(@PathVariable String publicId);
}

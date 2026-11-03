package com.altafjava.school.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.FeePaymentApi;
import com.altafjava.school.api.dto.request.BounceFeePaymentRequest;
import com.altafjava.school.api.dto.request.RecordFeePaymentRequest;
import com.altafjava.school.api.dto.response.FeeCollectionRowResponse;
import com.altafjava.school.api.dto.response.FeePaymentResponse;
import com.altafjava.school.api.mapper.FeePaymentMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.filter.FeePaymentFilter;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.FeeReceiptPdf;
import com.altafjava.school.application.service.FeeReceiptService;
import com.altafjava.school.domain.fee.model.PaymentMode;
import com.altafjava.school.domain.fee.model.PaymentSource;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@RestController
@RequestMapping("/api/v1/fee-payments")
public class FeePaymentController implements FeePaymentApi {

	private final FeePaymentService feePaymentService;
	private final FeePaymentMapper feePaymentMapper;
	private final FeeReceiptService feeReceiptService;

	private final SpringDataPageableResolver pageableResolver;

	private final ReferenceSummaries referenceSummaries;

	public FeePaymentController(FeePaymentService feePaymentService, FeePaymentMapper feePaymentMapper,
			FeeReceiptService feeReceiptService, SpringDataPageableResolver pageableResolver,
			ReferenceSummaries referenceSummaries) {
		this.feeReceiptService = feeReceiptService;
		this.referenceSummaries = referenceSummaries;
		this.feePaymentService = feePaymentService;
		this.feePaymentMapper = feePaymentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	@SortableBy({ "paidAt", "paidAmount", "receiptNumber" })
	public ApiResponse<com.altafjava.platform.core.model.Page<FeePaymentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) String feeStructurePublicId,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) PaymentSource paymentSource,
			@RequestParam(required = false) PaymentMode paymentMode) {
		FeePaymentFilter filter = new FeePaymentFilter(studentPublicId, feeStructurePublicId,
				new DateWindow(from, to), paymentSource, paymentMode);
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(referenceSummaries.mapPage(
						feePaymentService.listFeePayments(filter, pageableResolver.resolve(page, size)),
						feePaymentMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	public ApiResponse<FeePaymentResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(feePaymentMapper.toResponse(feePaymentService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	@RequireIdempotencyKey
	public ApiResponse<FeePaymentResponse> record(@Valid @RequestBody RecordFeePaymentRequest request) {
		return ApiResponse.success(feePaymentMapper.toResponse(feePaymentService.record(
				request.studentPublicId(),
				request.feeStructurePublicId(),
				request.paidAmount(),
				request.paidAt(),
				request.receiptNumber(),
				request.instrument(),
				request.invoicePublicId())));
	}

	@Override
	@GetMapping("/collection-report")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	public ApiResponse<List<FeeCollectionRowResponse>> collectionReport(@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		return ApiResponse.success(feePaymentService.collectionReport(from, to).stream()
				.map(row -> new FeeCollectionRowResponse(row.day(), row.paymentMode(), row.clearedCount(),
						row.clearedAmount(), row.bouncedCount(), row.bouncedAmount()))
				.toList());
	}

	@Override
	@PostMapping("/{publicId}/bounce")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	@RequireIdempotencyKey
	public ApiResponse<FeePaymentResponse> bounce(@PathVariable String publicId,
			@Valid @RequestBody BounceFeePaymentRequest request) {
		return ApiResponse.success(feePaymentMapper
				.toResponse(feePaymentService.bounce(publicId, request.reason(), request.bouncedAt())));
	}

	@Override
	@GetMapping("/{publicId}/receipt")
	@RequiresPermission(SchoolPermissions.FEE_PAYMENT_MANAGE)
	public ResponseEntity<byte[]> receipt(@PathVariable String publicId) {
		FeeReceiptPdf receipt = feeReceiptService.download(publicId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(receipt.fileName()).build().toString())
				.body(receipt.content());
	}
}

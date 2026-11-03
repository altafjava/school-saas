package com.altafjava.school.api.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.CancelFeeInvoiceRequest;
import com.altafjava.school.api.dto.request.IssueClassroomInvoicesRequest;
import com.altafjava.school.api.dto.request.IssueFeeInvoiceRequest;
import com.altafjava.school.api.dto.response.invoice.FeeInvoiceResponse;
import com.altafjava.school.api.dto.response.invoice.InvoiceBatchResponse;
import com.altafjava.school.api.mapper.FeeInvoiceMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.invoice.FeeInvoiceService;
import com.altafjava.school.application.invoice.InvoiceView;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.domain.fee.model.FeeInvoiceStatus;
import com.altafjava.school.domain.fee.model.InvoicePeriod;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Fee invoices", description = "Period invoices per student, with a verifiable PDF.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeeInvoiceController {

	private final FeeInvoiceService feeInvoiceService;
	private final FeeInvoiceMapper feeInvoiceMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@PostMapping("/fee-invoices")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@Operation(summary = "Issue an invoice", description = "Bills the student every installment, or whole fee without "
			+ "installments, that falls due in the period and is not on another live invoice.")
	public ApiResponse<FeeInvoiceResponse> issue(@Valid @RequestBody IssueFeeInvoiceRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(feeInvoiceMapper.toResponse(feeInvoiceService.issue(request.studentPublicId(),
				new InvoicePeriod(request.periodStart(), request.periodEnd(), request.dueDate()), user.getId())));
	}

	@PostMapping("/fee-invoices/classroom-runs")
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@Operation(summary = "Invoice a classroom", description = "Issues an invoice to every student of the classroom with "
			+ "something due in the period, and lists the students skipped with the reason.")
	public ApiResponse<InvoiceBatchResponse> issueForClassroom(
			@Valid @RequestBody IssueClassroomInvoicesRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		// One result, labelled in one batch across every invoice it holds.
		return ApiResponse.success(referenceSummaries.mapList(List.of(feeInvoiceService.issueForClassroom(
				request.classroomPublicId(),
				new InvoicePeriod(request.periodStart(), request.periodEnd(), request.dueDate()), user.getId())),
				result -> feeInvoiceMapper.toResponse(result)).getFirst());
	}

	@GetMapping("/fee-invoices")
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@SortableBy({ "invoiceNumber", "issueDate", "dueDate", "periodStart", "totalAmount" })
	@Operation(summary = "Invoices", description = "Every filter is optional; the dates match overlapping periods.")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeInvoiceResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(required = false) FeeInvoiceStatus status,
			@RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				feeInvoiceService.list(studentPublicId, status, new DateWindow(from, to),
						pageableResolver.resolve(page, size)),
				feeInvoiceMapper::toResponse)));
	}

	@GetMapping("/fee-invoices/{publicId}")
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@Operation(summary = "Invoice")
	public ApiResponse<FeeInvoiceResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(feeInvoiceMapper.toResponse(feeInvoiceService.find(publicId)));
	}

	@GetMapping("/fee-invoices/{publicId}/pdf")
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@Operation(summary = "Invoice PDF")
	public ResponseEntity<byte[]> pdf(@PathVariable String publicId) {
		return pdfOf(feeInvoiceService.find(publicId));
	}

	@PostMapping("/fee-invoices/{publicId}/cancel")
	@RequiresPermission(SchoolPermissions.FEE_INVOICE_MANAGE)
	@Operation(summary = "Cancel an invoice", description = "Refused while payments stand against it. Its charges can "
			+ "then be billed again.")
	public ApiResponse<FeeInvoiceResponse> cancel(@PathVariable String publicId,
			@Valid @RequestBody CancelFeeInvoiceRequest request) {
		return ApiResponse.success(feeInvoiceMapper.toResponse(feeInvoiceService.cancel(publicId, request.reason())));
	}

	@GetMapping("/students/{studentPublicId}/fee-invoices")
	@RequiresPermission(SchoolPermissions.STUDENT_FEE_BALANCE_READ)
	@SortableBy({ "issueDate", "dueDate", "periodStart" })
	@Operation(summary = "A student's invoices", description = "For the student, their guardians and fee staff.")
	public ApiResponse<com.altafjava.platform.core.model.Page<FeeInvoiceResponse>> listForStudent(
			@PathVariable String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				feeInvoiceService.listForStudent(studentPublicId, pageableResolver.resolve(page, size)),
				feeInvoiceMapper::toResponse)));
	}

	@GetMapping("/students/{studentPublicId}/fee-invoices/{publicId}/pdf")
	@RequiresPermission(SchoolPermissions.STUDENT_FEE_BALANCE_READ)
	@Operation(summary = "A student's invoice PDF")
	public ResponseEntity<byte[]> studentPdf(@PathVariable String studentPublicId, @PathVariable String publicId) {
		return pdfOf(feeInvoiceService.findForStudent(studentPublicId, publicId));
	}

	private ResponseEntity<byte[]> pdfOf(InvoiceView view) {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(view.invoice().getInvoiceNumber() + ".pdf").build().toString())
				.body(feeInvoiceService.pdf(view.invoice()));
	}
}

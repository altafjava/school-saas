package com.altafjava.school.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.PayslipApi;
import com.altafjava.school.api.dto.response.PayslipResponse;
import com.altafjava.school.api.mapper.PayslipMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.filter.PayslipFilter;
import com.altafjava.school.application.service.PayslipService;
import com.altafjava.school.domain.payroll.model.PayslipStatus;

@RestController
@RequestMapping("/api/v1/payslips")
public class PayslipController implements PayslipApi {

	private final PayslipService payslipService;
	private final PayslipMapper payslipMapper;

	private final SpringDataPageableResolver pageableResolver;

	public PayslipController(PayslipService payslipService, PayslipMapper payslipMapper,
			SpringDataPageableResolver pageableResolver) {
		this.payslipService = payslipService;
		this.payslipMapper = payslipMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAYSLIP_MANAGE')")
	@SortableBy({ "payYear", "payMonth", "status", "netPay" })
	public ApiResponse<com.altafjava.platform.core.model.Page<PayslipResponse>> list(
			@RequestParam(required = false) String employeePublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Integer payYear,
			@RequestParam(required = false) Integer payMonth,
			@RequestParam(required = false) PayslipStatus status) {
		PayslipFilter filter = new PayslipFilter(employeePublicId, payYear, payMonth, status);
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				payslipService.list(filter, pageableResolver.resolve(page, size)).map(payslipMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAYSLIP_MANAGE')")
	public ApiResponse<PayslipResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(payslipMapper.toResponse(payslipService.findByPublicId(publicId)));
	}

	@Override
	@PatchMapping("/{publicId}/finalize")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAYSLIP_MANAGE')")
	@RequireIdempotencyKey
	public ApiResponse<PayslipResponse> finalizePayslip(@PathVariable String publicId) {
		return ApiResponse.success(payslipMapper.toResponse(payslipService.finalizePayslip(publicId)));
	}

	// Disbursement is the finance action of actually paying out — gated separately from the HR
	// actions above.
	@Override
	@PatchMapping("/{publicId}/disburse")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('PAYSLIP_DISBURSE')")
	@RequireIdempotencyKey
	public ApiResponse<PayslipResponse> disburse(@PathVariable String publicId) {
		return ApiResponse.success(payslipMapper.toResponse(payslipService.markDisbursed(publicId)));
	}
}

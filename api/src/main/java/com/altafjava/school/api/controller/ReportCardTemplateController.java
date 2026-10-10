package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.controller.api.ReportCardTemplateApi;
import com.altafjava.school.api.dto.request.ConfigureReportCardTemplateRequest;
import com.altafjava.school.api.dto.response.ReportCardTemplateResponse;
import com.altafjava.school.api.mapper.ReportCardTemplateMapper;
import com.altafjava.school.application.service.ReportCardTemplateService;

// One config per tenant — which optional sections ReportCardPdfGenerator renders. Read is broad
// (teachers need to know what a generated report card will contain); writes are tenant-admin-only.
@RestController
@RequestMapping("/api/v1/report-card-template")
public class ReportCardTemplateController implements ReportCardTemplateApi {

	private final ReportCardTemplateService reportCardTemplateService;
	private final ReportCardTemplateMapper reportCardTemplateMapper;

	public ReportCardTemplateController(ReportCardTemplateService reportCardTemplateService,
			ReportCardTemplateMapper reportCardTemplateMapper) {
		this.reportCardTemplateService = reportCardTemplateService;
		this.reportCardTemplateMapper = reportCardTemplateMapper;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('REPORT_CARD_TEMPLATE_READ')")
	public ApiResponse<ReportCardTemplateResponse> get() {
		return ApiResponse
				.success(reportCardTemplateMapper.toResponse(reportCardTemplateService.getForCurrentTenant()));
	}

	@Override
	@PutMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('REPORT_CARD_TEMPLATE_WRITE')")
	public ApiResponse<ReportCardTemplateResponse> configure(
			@Valid @RequestBody ConfigureReportCardTemplateRequest request) {
		return ApiResponse.success(reportCardTemplateMapper.toResponse(reportCardTemplateService.configure(
				request.showAttendanceSummary(), request.showRemarks(), request.showCompetencyGrid(),
				request.showRank(), request.expectedVersion())));
	}
}

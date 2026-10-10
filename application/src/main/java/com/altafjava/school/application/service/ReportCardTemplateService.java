package com.altafjava.school.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.reportcard.model.ReportCardTemplate;
import com.altafjava.school.domain.reportcard.repository.ReportCardTemplateRepository;

@Service
public class ReportCardTemplateService {

	private final ReportCardTemplateRepository reportCardTemplateRepository;

	public ReportCardTemplateService(ReportCardTemplateRepository reportCardTemplateRepository) {
		this.reportCardTemplateRepository = reportCardTemplateRepository;
	}

	// Never persists the default — a tenant that never configures a template keeps costing zero
	// extra rows and always reads the same all-false default.
	@Transactional(readOnly = true)
	public ReportCardTemplate getForCurrentTenant() {
		return reportCardTemplateRepository.findByTenantId(TenantContext.getCurrentTenantId())
				.orElseGet(ReportCardTemplate::createDefault);
	}

	@Transactional
	public ReportCardTemplate configure(boolean showAttendanceSummary, boolean showRemarks,
			boolean showCompetencyGrid, boolean showRank, ExpectedVersion expectedVersion) {
		ReportCardTemplate template = reportCardTemplateRepository
				.findByTenantId(TenantContext.getCurrentTenantId())
				.orElseGet(ReportCardTemplate::createDefault);
		expectedVersion.verify(template);
		template.configure(showAttendanceSummary, showRemarks, showCompetencyGrid, showRank);
		return reportCardTemplateRepository.save(template);
	}
}

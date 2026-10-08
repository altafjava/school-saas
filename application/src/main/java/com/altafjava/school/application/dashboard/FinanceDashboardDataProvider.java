package com.altafjava.school.application.dashboard;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeRefundRepository;
import com.altafjava.school.domain.library.repository.CirculationRepository;

/** Finance summary: one aggregate row, computed from single SUM/COUNT queries. */
@Component
public class FinanceDashboardDataProvider implements ReportDataProvider {

	private final FeePaymentRepository feePaymentRepository;
	private final CirculationRepository circulationRepository;
	private final FeeRefundRepository feeRefundRepository;

	public FinanceDashboardDataProvider(FeePaymentRepository feePaymentRepository,
			CirculationRepository circulationRepository, FeeRefundRepository feeRefundRepository) {
		this.feeRefundRepository = feeRefundRepository;
		this.feePaymentRepository = feePaymentRepository;
		this.circulationRepository = circulationRepository;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();

		Map<String, Object> row = new LinkedHashMap<>();
		BigDecimal collected = feePaymentRepository.sumPaidAmountByTenantId(tenantId);
		BigDecimal refunded = feeRefundRepository.sumCompletedByTenantId(tenantId);
		row.put("totalFeeCollected", collected);
		row.put("totalFeeRefunded", refunded);
		row.put("netFeeCollected", collected.subtract(refunded));
		row.put("feePaymentCount", feePaymentRepository.countByTenantId(tenantId));
		row.put("totalLibraryFinesCollected", circulationRepository.sumFineAmountByTenantId(tenantId));
		return List.of(row);
	}
}

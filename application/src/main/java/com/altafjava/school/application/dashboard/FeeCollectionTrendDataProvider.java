package com.altafjava.school.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;

/**
 * Monthly fee-collection trend, one row per month — used by the Finance dashboard's {@code
 * /trends} endpoint. {@code parameters.periods} (default 6) controls how many trailing months to
 * return.
 */
@Component
public class FeeCollectionTrendDataProvider implements ReportDataProvider {

	private static final int DEFAULT_PERIODS = 6;
	private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private final FeePaymentRepository feePaymentRepository;
	private final SchoolClock schoolClock;

	public FeeCollectionTrendDataProvider(FeePaymentRepository feePaymentRepository, SchoolClock schoolClock) {
		this.feePaymentRepository = feePaymentRepository;
		this.schoolClock = schoolClock;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();
		int periods = periodCount(parameters);
		YearMonth currentMonth = schoolClock.currentMonth(tenantId);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (int i = periods - 1; i >= 0; i--) {
			rows.add(monthRow(tenantId, currentMonth.minusMonths(i)));
		}
		return rows;
	}

	private Map<String, Object> monthRow(Long tenantId, YearMonth month) {
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = LocalDate.from(month.atEndOfMonth()).atTime(23, 59, 59);
		BigDecimal totalCollected = feePaymentRepository.sumClearedAmountByTenantIdAndPaidAtBetween(tenantId, from, to);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("month", month.format(MONTH_LABEL_FORMAT));
		row.put("totalCollected", totalCollected);
		return row;
	}

	private int periodCount(Map<String, Object> parameters) {
		Object periods = parameters.get("periods");
		if (periods instanceof Number number && number.intValue() > 0) {
			return number.intValue();
		}
		return DEFAULT_PERIODS;
	}
}

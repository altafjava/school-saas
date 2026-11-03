package com.altafjava.school.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;

/**
 * Monthly leave-utilization trend, one row per month — used by the HR dashboard's {@code /trends}
 * endpoint. {@code parameters.periods} (default 6) controls how many trailing months to return.
 */
@Component
public class LeaveUtilizationTrendDataProvider implements ReportDataProvider {

	private static final int DEFAULT_PERIODS = 6;
	private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

	private final LeaveRequestRepository leaveRequestRepository;
	private final SchoolClock schoolClock;

	public LeaveUtilizationTrendDataProvider(LeaveRequestRepository leaveRequestRepository, SchoolClock schoolClock) {
		this.leaveRequestRepository = leaveRequestRepository;
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
		LocalDate from = month.atDay(1);
		LocalDate to = month.atEndOfMonth();
		BigDecimal daysTaken = leaveRequestRepository.sumDaysRequestedByTenantIdAndStatusAndStartDateBetween(tenantId,
				LeaveRequestStatus.APPROVED, from, to);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("month", month.format(MONTH_LABEL_FORMAT));
		row.put("approvedLeaveDays", daysTaken);
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

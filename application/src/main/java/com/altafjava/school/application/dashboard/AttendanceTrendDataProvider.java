package com.altafjava.school.application.dashboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.attendance.model.AttendanceStatusCount;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;

/**
 * Weekly attendance-percentage trend, one row per week — used by both the Principal and Academic
 * dashboards' {@code /trends} endpoint. {@code parameters.periods} (default 8) controls how many
 * trailing weeks to return.
 */
@Component
public class AttendanceTrendDataProvider implements ReportDataProvider {

	private static final int DEFAULT_PERIODS = 8;
	private static final DateTimeFormatter WEEK_LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final AttendanceRepository attendanceRepository;
	private final SchoolClock schoolClock;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();

	public AttendanceTrendDataProvider(AttendanceRepository attendanceRepository, SchoolClock schoolClock) {
		this.attendanceRepository = attendanceRepository;
		this.schoolClock = schoolClock;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();
		int periods = periodCount(parameters);
		LocalDate weekEnd = schoolClock.today(tenantId);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (int i = periods - 1; i >= 0; i--) {
			LocalDate to = weekEnd.minusWeeks(i);
			LocalDate from = to.minusDays(6);
			rows.add(weekRow(tenantId, from, to));
		}
		return rows;
	}

	private Map<String, Object> weekRow(Long tenantId, LocalDate from, LocalDate to) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("weekStart", from.format(WEEK_LABEL_FORMAT));
		row.put("weekEnd", to.format(WEEK_LABEL_FORMAT));
		row.put("attendancePercentage", attendancePercentageCalculator
				.calculate(AttendanceStatusCount.byStatus(attendanceRepository.countByStatus(tenantId, from, to)))
				.percentage());
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

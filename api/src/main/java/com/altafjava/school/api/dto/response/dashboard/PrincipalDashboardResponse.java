package com.altafjava.school.api.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.Map;

public record PrincipalDashboardResponse(long activeStudentCount, BigDecimal attendancePercentageLast30Days,
		long upcomingEventCount, Map<String, Integer> activeAlertCounts) {

	public PrincipalDashboardResponse {
		activeAlertCounts = Map.copyOf(activeAlertCounts);
	}
}

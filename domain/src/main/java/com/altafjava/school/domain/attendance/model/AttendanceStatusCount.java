package com.altafjava.school.domain.attendance.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** How many registers hold one status. */
public record AttendanceStatusCount(AttendanceStatus status, long days) {

	public static Map<AttendanceStatus, Long> byStatus(Collection<AttendanceStatusCount> counts) {
		Map<AttendanceStatus, Long> days = new EnumMap<>(AttendanceStatus.class);
		counts.forEach(count -> days.merge(count.status(), count.days(), Long::sum));
		return days;
	}
}

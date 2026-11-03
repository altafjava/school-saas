package com.altafjava.school.domain.attendance.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** How many days one student holds one status. */
public record AttendanceTally(Long studentId, AttendanceStatus status, long days) {

	public static Map<AttendanceStatus, Long> byStatus(Collection<AttendanceTally> tallies) {
		Map<AttendanceStatus, Long> days = new EnumMap<>(AttendanceStatus.class);
		tallies.forEach(tally -> days.merge(tally.status(), tally.days(), Long::sum));
		return days;
	}
}

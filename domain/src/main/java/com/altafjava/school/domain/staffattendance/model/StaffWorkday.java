package com.altafjava.school.domain.staffattendance.model;

import java.time.LocalTime;
import com.altafjava.platform.core.exception.BusinessException;

/**
 * A school's working day for staff: arriving after {@code start} plus the grace is late, leaving
 * before {@code end} is early, and fewer than {@code halfDayMinutes} worked is a half day.
 */
public record StaffWorkday(LocalTime start, LocalTime end, int lateGraceMinutes, int halfDayMinutes) {

	public static final StaffWorkday DEFAULT = new StaffWorkday(LocalTime.of(9, 0), LocalTime.of(17, 0), 10, 240);

	public StaffWorkday {
		if (start == null || end == null || !start.isBefore(end)) {
			throw new BusinessException("The workday must start before it ends");
		}
		if (lateGraceMinutes < 0 || halfDayMinutes < 1) {
			throw new BusinessException("Grace minutes cannot be negative and a half day needs at least a minute");
		}
	}

	public boolean isLate(LocalTime arrival) {
		return arrival.isAfter(start.plusMinutes(lateGraceMinutes));
	}

	public boolean isEarly(LocalTime departure) {
		return departure.isBefore(end);
	}
}

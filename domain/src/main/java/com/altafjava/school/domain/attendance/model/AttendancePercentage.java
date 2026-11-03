package com.altafjava.school.domain.attendance.model;

import java.math.BigDecimal;

/** {@code presentDays} can be fractional, since a half day counts as half. */
public record AttendancePercentage(BigDecimal presentDays, long totalMarkedDays, BigDecimal percentage) {
}

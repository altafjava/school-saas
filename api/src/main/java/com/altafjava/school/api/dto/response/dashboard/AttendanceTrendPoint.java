package com.altafjava.school.api.dto.response.dashboard;

import java.math.BigDecimal;

public record AttendanceTrendPoint(String weekStart, String weekEnd, BigDecimal attendancePercentage) {
}

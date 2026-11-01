package com.altafjava.school.api.dto.response.dashboard;

import java.math.BigDecimal;

public record LeaveUtilizationTrendPoint(String month, BigDecimal approvedLeaveDays) {
}

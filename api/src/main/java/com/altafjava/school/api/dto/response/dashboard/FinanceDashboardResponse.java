package com.altafjava.school.api.dto.response.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardResponse(BigDecimal totalFeeCollected, BigDecimal totalFeeRefunded,
		BigDecimal netFeeCollected,
		long feePaymentCount, BigDecimal totalLibraryFinesCollected) {
}

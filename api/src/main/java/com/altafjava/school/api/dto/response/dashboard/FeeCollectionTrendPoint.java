package com.altafjava.school.api.dto.response.dashboard;

import java.math.BigDecimal;

public record FeeCollectionTrendPoint(String month, BigDecimal totalCollected) {
}

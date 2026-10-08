package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeInstallmentResponse(int sequenceNumber, LocalDate dueDate, BigDecimal sharePercentage) {
}

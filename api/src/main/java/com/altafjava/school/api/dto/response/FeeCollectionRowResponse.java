package com.altafjava.school.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.altafjava.school.domain.fee.model.PaymentMode;

public record FeeCollectionRowResponse(
		LocalDate day,
		PaymentMode paymentMode,
		long clearedCount,
		BigDecimal clearedAmount,
		long bouncedCount,
		BigDecimal bouncedAmount) {
}

package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day's payments in one mode, for matching against the cash box and the bank statement. */
public record FeeCollectionRow(LocalDate day, PaymentMode paymentMode, long clearedCount, BigDecimal clearedAmount,
		long bouncedCount, BigDecimal bouncedAmount) {
}

package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One installment as it stands today, with payments allocated oldest-first. */
public record InstallmentStatus(int sequenceNumber, LocalDate dueDate, BigDecimal amount, BigDecimal paidAmount,
		BigDecimal outstandingAmount, State state) {

	public enum State {
		PAID, PARTIALLY_PAID, OVERDUE, UPCOMING
	}
}

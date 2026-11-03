package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;

/** How much of an invoice has been paid by cleared payments, and what is left. */
public record InvoiceSettlement(BigDecimal paid, BigDecimal outstanding, State state) {

	public enum State {
		UNPAID, PARTIALLY_PAID, PAID, CANCELLED
	}
}

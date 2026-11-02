package com.altafjava.school.domain.fee.model;

/** How the money reached the school. {@link PaymentSource} says who recorded it. */
public enum PaymentMode {
	CASH, CHEQUE, UPI, BANK_TRANSFER, CARD, GATEWAY
}

package com.altafjava.school.domain.fee.model;

public enum RefundMethod {
	// Money handed back outside the system (cash, cheque, bank transfer) — recorded, not executed.
	MANUAL,
	// Executed through the tenant's payment gateway against the original charge.
	GATEWAY
}

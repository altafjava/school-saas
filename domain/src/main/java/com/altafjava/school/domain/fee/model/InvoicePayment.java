package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;

/** Cleared payments made against one invoice. */
public record InvoicePayment(Long invoiceId, BigDecimal paid) {
}

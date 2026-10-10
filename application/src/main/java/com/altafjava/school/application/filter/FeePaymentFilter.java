package com.altafjava.school.application.filter;

import com.altafjava.school.domain.fee.model.PaymentSource;

/**
 * Narrows the fee-payment list; every part is optional. A payment's "method" is its source (recorded by staff or
 * confirmed through the gateway); refunds are a separate resource.
 */
public record FeePaymentFilter(String studentPublicId, String feeStructurePublicId, DateWindow dates,
		PaymentSource paymentSource) {

	public static final FeePaymentFilter NONE = new FeePaymentFilter(null, null, DateWindow.UNBOUNDED, null);
}

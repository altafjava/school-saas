package com.altafjava.school.domain.fee.model;

import java.time.LocalDate;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.error.FeeErrorCode;

/** The days an invoice covers, inclusive, and when it must be paid. */
public record InvoicePeriod(LocalDate start, LocalDate end, LocalDate dueDate) {

	public InvoicePeriod {
		if (start == null || end == null || dueDate == null) {
			throw new BusinessException(FeeErrorCode.INVOICE_PERIOD_INVALID,
					"An invoice needs a start, an end and a due date");
		}
		if (end.isBefore(start)) {
			throw new BusinessException(FeeErrorCode.INVOICE_PERIOD_INVALID,
					"The invoice period cannot end before it starts");
		}
	}

	/** Whether a charge due on {@code date} belongs here; one with no due date belongs to any period. */
	public boolean covers(LocalDate date) {
		return date == null || !date.isBefore(start) && !date.isAfter(end);
	}
}

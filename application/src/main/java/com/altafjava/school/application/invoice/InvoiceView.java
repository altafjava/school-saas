package com.altafjava.school.application.invoice;

import java.util.List;
import com.altafjava.school.domain.fee.model.FeeInvoice;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;
import com.altafjava.school.domain.fee.model.InvoiceSettlement;

/** An invoice with its lines, read inside the transaction, and what its cleared payments have settled. */
public record InvoiceView(FeeInvoice invoice, List<FeeInvoiceLine> lines, InvoiceSettlement settlement) {

	public InvoiceView {
		lines = List.copyOf(lines);
	}

	public static InvoiceView of(FeeInvoice invoice, InvoiceSettlement settlement) {
		return new InvoiceView(invoice, invoice.getLines(), settlement);
	}
}

package com.altafjava.school.application.invoice;

import java.util.List;

/** What a classroom run produced: the invoices issued, and why each other student got none. */
public record InvoiceBatchResult(List<InvoiceView> issued, List<Skipped> skipped) {

	public InvoiceBatchResult {
		issued = List.copyOf(issued);
		skipped = List.copyOf(skipped);
	}

	public record Skipped(String studentPublicId, String reason) {
	}
}

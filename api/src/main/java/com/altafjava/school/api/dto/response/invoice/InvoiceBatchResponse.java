package com.altafjava.school.api.dto.response.invoice;

import java.util.List;
import com.altafjava.school.api.support.NoLabel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The invoices a classroom run issued, and each student it skipped with the reason.")
public record InvoiceBatchResponse(List<FeeInvoiceResponse> issued, List<Skipped> skipped) {

	public InvoiceBatchResponse {
		issued = issued == null ? null : List.copyOf(issued);
		skipped = skipped == null ? null : List.copyOf(skipped);
	}

	public record Skipped(@NoLabel("The roster already names the student") String studentPublicId, String reason) {
	}
}

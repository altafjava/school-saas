package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.school.domain.fee.model.PaymentInstrument;
import com.altafjava.school.domain.fee.model.PaymentMode;

// receiptNumber is an explicit-override path — omit it (null/blank) to have the tenant's
// configured numbering sequence generate one; see FeePaymentService#record.
public record RecordFeePaymentRequest(
		@NotBlank String studentPublicId,
		@NotBlank String feeStructurePublicId,
		@NotNull @DecimalMin("0.01") BigDecimal paidAmount,
		@NotNull LocalDateTime paidAt,
		@Size(max = 100) String receiptNumber,
		@NotNull PaymentMode paymentMode,
		@Size(max = 100) String transactionReference,
		@Size(max = 50) String chequeNumber,
		LocalDate chequeDate,
		@Size(max = 100) String chequeBank) {

	public PaymentInstrument instrument() {
		return new PaymentInstrument(paymentMode, transactionReference, chequeNumber, chequeDate, chequeBank);
	}
}

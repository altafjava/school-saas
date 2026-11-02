package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class PaymentInstrumentTest {

	private static final LocalDate DATE = LocalDate.of(2026, 11, 2);

	@Test
	void aPaymentNeedsAMode() {
		assertThrows(BusinessException.class, () -> new PaymentInstrument(null, null, null, null, null));
	}

	@Test
	void cash_needsNoDetails() {
		assertDoesNotThrow(() -> new PaymentInstrument(PaymentMode.CASH, null, null, null, null));
	}

	@Test
	void cheque_needsNumberDateAndBank() {
		assertDoesNotThrow(() -> new PaymentInstrument(PaymentMode.CHEQUE, null, "0045", DATE, "SBI"));
		assertThrows(BusinessException.class,
				() -> new PaymentInstrument(PaymentMode.CHEQUE, null, " ", DATE, "SBI"));
		assertThrows(BusinessException.class,
				() -> new PaymentInstrument(PaymentMode.CHEQUE, null, "0045", null, "SBI"));
		assertThrows(BusinessException.class,
				() -> new PaymentInstrument(PaymentMode.CHEQUE, null, "0045", DATE, null));
	}

	@Test
	void electronicModes_needAReference() {
		for (PaymentMode mode : List.of(PaymentMode.UPI, PaymentMode.BANK_TRANSFER, PaymentMode.CARD)) {
			assertDoesNotThrow(() -> new PaymentInstrument(mode, "REF-1", null, null, null));
			assertThrows(BusinessException.class, () -> new PaymentInstrument(mode, null, null, null, null));
		}
	}
}

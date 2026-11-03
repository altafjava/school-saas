package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class FeeInvoiceTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 1);
	private static final InvoicePeriod NOVEMBER = new InvoicePeriod(START, START.plusMonths(1).minusDays(1),
			START.plusDays(10));

	@Test
	void issue_totalsTheLines() {
		FeeInvoice invoice = invoice();

		assertEquals(0, new BigDecimal("1500").compareTo(invoice.getTotalAmount()));
		assertEquals(FeeInvoiceStatus.ISSUED, invoice.getStatus());
	}

	@Test
	void issue_withNothingToBill_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> FeeInvoice.issue(1L, "INV-1", NOVEMBER, START, List.of()));
	}

	@Test
	void issue_dueBeforeItIsIssued_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> FeeInvoice.issue(1L, "INV-1", NOVEMBER, START.plusDays(11),
				List.of(line(10L, null, "1000"))));
	}

	@Test
	void period_endingBeforeItStarts_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> new InvoicePeriod(START, START.minusDays(1), START));
	}

	@Test
	void period_coversItsOwnDaysAndChargesWithoutADueDate() {
		assertTrue(NOVEMBER.covers(START));
		assertTrue(NOVEMBER.covers(null));
		assertFalse(NOVEMBER.covers(START.plusMonths(1)));
	}

	@Test
	void settlement_movesFromUnpaidToPaid() {
		FeeInvoice invoice = invoice();

		assertEquals(InvoiceSettlement.State.UNPAID, invoice.settlement(BigDecimal.ZERO).state());
		InvoiceSettlement part = invoice.settlement(new BigDecimal("500"));
		assertEquals(InvoiceSettlement.State.PARTIALLY_PAID, part.state());
		assertEquals(0, new BigDecimal("1000").compareTo(part.outstanding()));
		assertEquals(InvoiceSettlement.State.PAID, invoice.settlement(new BigDecimal("1500")).state());
	}

	@Test
	void cancel_twice_throwsBusinessException_andACancelledInvoiceOwesNothing() {
		FeeInvoice invoice = invoice();
		invoice.cancel("Wrong period");

		assertThrows(BusinessException.class, () -> invoice.cancel("again"));
		assertEquals(InvoiceSettlement.State.CANCELLED, invoice.settlement(BigDecimal.ZERO).state());
		assertEquals(0, BigDecimal.ZERO.compareTo(invoice.settlement(BigDecimal.ZERO).outstanding()));
	}

	@Test
	void payment_againstAFeeTheInvoiceDoesNotBill_isRefused() {
		FeeInvoice invoice = invoice();
		FeePayment payment = FeePayment.create(1L, 99L, BigDecimal.TEN, LocalDateTime.now(), "R-1",
				PaymentInstrument.CASH);

		assertThrows(BusinessException.class, () -> payment.applyTo(invoice));
	}

	@Test
	void payment_byAnotherStudent_isRefused() {
		FeeInvoice invoice = invoice();
		FeePayment payment = FeePayment.create(2L, 10L, BigDecimal.TEN, LocalDateTime.now(), "R-1",
				PaymentInstrument.CASH);

		assertThrows(BusinessException.class, () -> payment.applyTo(invoice));
	}

	@Test
	void payment_againstACancelledInvoice_isRefused() {
		FeeInvoice invoice = invoice();
		invoice.cancel("Wrong period");
		FeePayment payment = FeePayment.create(1L, 10L, BigDecimal.TEN, LocalDateTime.now(), "R-1",
				PaymentInstrument.CASH);

		assertThrows(BusinessException.class, () -> payment.applyTo(invoice));
	}

	private static FeeInvoice invoice() {
		return FeeInvoice.issue(1L, "INV-1", NOVEMBER, START,
				List.of(line(10L, null, "1000"), line(11L, 1, "500")));
	}

	private static FeeInvoiceLine line(Long structureId, Integer installment, String amount) {
		return new FeeInvoiceLine(structureId, installment, "Fee " + structureId, new BigDecimal(amount));
	}
}

package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class FeePaymentTest {

	@Test
	void create_defaultsToManualPaymentSourceWithNoGatewayFields() {
		FeePayment payment = FeePayment.create(1L, 2L, BigDecimal.valueOf(500), LocalDateTime.now(), "RCPT-1",
				PaymentInstrument.CASH);

		assertEquals(PaymentSource.MANUAL, payment.getPaymentSource());
		assertNull(payment.getGatewayProviderType());
		assertNull(payment.getGatewayChargeReference());
	}

	@Test
	void recordFromGateway_setsGatewaySourceAndFields() {
		LocalDateTime paidAt = LocalDateTime.of(2026, 3, 1, 10, 0);

		FeePayment payment = FeePayment.recordFromGateway(1L, 2L, BigDecimal.valueOf(500), paidAt, "GTW-pi_123",
				"STRIPE", "pi_123");

		assertEquals(1L, payment.getStudentId());
		assertEquals(2L, payment.getFeeStructureId());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(payment.getPaidAmount()));
		assertEquals(paidAt, payment.getPaidAt());
		assertEquals("GTW-pi_123", payment.getReceiptNumber());
		assertEquals(PaymentSource.GATEWAY, payment.getPaymentSource());
		assertEquals("STRIPE", payment.getGatewayProviderType());
		assertEquals("pi_123", payment.getGatewayChargeReference());
	}

	@Test
	void bouncedCheque_keepsTheRowButIsMarkedBounced() {
		FeePayment payment = cheque();
		LocalDateTime bouncedAt = LocalDateTime.of(2026, 11, 2, 10, 0);

		payment.bounce("Insufficient funds", bouncedAt);

		assertTrue(payment.isBounced());
		assertEquals(FeePaymentStatus.BOUNCED, payment.getStatus());
		assertEquals(bouncedAt, payment.getBouncedAt());
		assertEquals("Insufficient funds", payment.getBounceReason());
	}

	@Test
	void chequeCannotBounceTwice() {
		FeePayment payment = cheque();
		payment.bounce("Insufficient funds", LocalDateTime.now());

		assertThrows(BusinessException.class, () -> payment.bounce("Again", LocalDateTime.now()));
	}

	@Test
	void onlyAChequeCanBounce() {
		FeePayment cash = FeePayment.create(1L, 2L, BigDecimal.TEN, LocalDateTime.now(), "R-1", PaymentInstrument.CASH);

		assertThrows(BusinessException.class, () -> cash.bounce("No", LocalDateTime.now()));
	}

	private static FeePayment cheque() {
		return FeePayment.create(1L, 2L, BigDecimal.TEN, LocalDateTime.now(), "R-2",
				new PaymentInstrument(PaymentMode.CHEQUE, null, "0045", LocalDate.of(2026, 11, 1), "SBI"));
	}
}

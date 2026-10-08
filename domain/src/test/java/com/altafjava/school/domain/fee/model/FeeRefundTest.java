package com.altafjava.school.domain.fee.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class FeeRefundTest {

	private FeePayment payment() {
		FeePayment payment = FeePayment.create(5L, 6L, new BigDecimal("500.00"), LocalDateTime.now(), "RCPT-1");
		payment.setId(7L);
		return payment;
	}

	@Test
	void manualRefund_isCompletedImmediately_andCopiesThePaymentsKeys() {
		FeeRefund refund = FeeRefund.reserve(payment(), new BigDecimal("100"), "overcharged", RefundMethod.MANUAL,
				"CN-1", 9L);

		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
		assertEquals(7L, refund.getFeePaymentId());
		assertEquals(5L, refund.getStudentId());
		assertEquals(6L, refund.getFeeStructureId());
	}

	@Test
	void gatewayRefund_startsPending_andPendingStillReservesTheAmount() {
		FeeRefund refund = FeeRefund.reserve(payment(), new BigDecimal("100"), "x", RefundMethod.GATEWAY, "CN-2", 9L);

		assertEquals(RefundStatus.PENDING, refund.getStatus());
		assertTrue(refund.isReserving());
	}

	@Test
	void complete_recordsTheGatewayReference() {
		FeeRefund refund = FeeRefund.reserve(payment(), new BigDecimal("100"), "x", RefundMethod.GATEWAY, "CN-2", 9L);

		refund.complete("re_123");

		assertEquals(RefundStatus.COMPLETED, refund.getStatus());
		assertEquals("re_123", refund.getGatewayRefundReference());
	}

	@Test
	void failedRefund_releasesItsReservation() {
		FeeRefund refund = FeeRefund.reserve(payment(), new BigDecimal("100"), "x", RefundMethod.GATEWAY, "CN-2", 9L);

		refund.fail("card closed");

		assertFalse(refund.isReserving());
		assertEquals("card closed", refund.getFailureReason());
	}

	@Test
	void settledRefund_cannotBeSettledAgain() {
		FeeRefund refund = FeeRefund.reserve(payment(), new BigDecimal("100"), "x", RefundMethod.GATEWAY, "CN-2", 9L);
		refund.complete("re_1");

		assertThrows(BusinessException.class, () -> refund.fail("late"));
		assertThrows(BusinessException.class, () -> refund.complete("re_2"));
	}

	@Test
	void reserve_rejectsNonPositiveAmounts() {
		assertThrows(BusinessException.class,
				() -> FeeRefund.reserve(payment(), BigDecimal.ZERO, "x", RefundMethod.MANUAL, "CN-3", 9L));
	}
}

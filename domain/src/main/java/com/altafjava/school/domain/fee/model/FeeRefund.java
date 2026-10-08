package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.TenantEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Money returned against one {@link FeePayment}. Never deleted: it is the accounting record of a
 * reversal, identified by its own credit-note number. A gateway refund is first reserved as
 * {@code PENDING} (so it counts against what is refundable even if the process dies mid-call),
 * then settled to {@code COMPLETED} or {@code FAILED}. Only {@code COMPLETED} refunds reduce what
 * the student is considered to have paid.
 */
@Entity
@Table(name = "fee_refunds")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeRefund extends TenantEntity {

	// FK to fee_payments.id
	@Column(name = "fee_payment_id", nullable = false)
	private Long feePaymentId;

	// FK to students.id and fee_structures.id — denormalized from the payment so balances can sum
	// refunds without joining through payments.
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "reason", nullable = false, length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "method", nullable = false, length = 20)
	private RefundMethod method;

	@Column(name = "credit_note_number", nullable = false, length = 100)
	private String creditNoteNumber;

	@Column(name = "gateway_refund_reference", length = 255)
	private String gatewayRefundReference;

	@Column(name = "failure_reason", length = 500)
	private String failureReason;

	// FK to platform users.id
	@Column(name = "refunded_by_user_id")
	private Long refundedByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private RefundStatus status;

	public static FeeRefund reserve(FeePayment payment, BigDecimal amount, String reason, RefundMethod method,
			String creditNoteNumber, Long refundedByUserId) {
		if (amount == null || amount.signum() <= 0) {
			throw new BusinessException("Refund amount must be greater than zero");
		}
		return FeeRefund.builder()
				.feePaymentId(payment.getId())
				.studentId(payment.getStudentId())
				.feeStructureId(payment.getFeeStructureId())
				.amount(amount)
				.reason(reason)
				.method(method)
				.status(method == RefundMethod.MANUAL ? RefundStatus.COMPLETED : RefundStatus.PENDING)
				.creditNoteNumber(creditNoteNumber)
				.refundedByUserId(refundedByUserId)
				.build();
	}

	public void complete(String gatewayRefundReference) {
		requirePending();
		this.status = RefundStatus.COMPLETED;
		this.gatewayRefundReference = gatewayRefundReference;
	}

	public void fail(String failureReason) {
		requirePending();
		this.status = RefundStatus.FAILED;
		this.failureReason = failureReason;
	}

	/** Counts against the payment's refundable amount: settled, or still in flight. */
	public boolean isReserving() {
		return status != RefundStatus.FAILED;
	}

	private void requirePending() {
		if (status != RefundStatus.PENDING) {
			throw new BusinessException("Only a pending refund can be settled, was " + status);
		}
	}
}

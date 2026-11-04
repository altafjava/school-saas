package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for fees, invoices, payments and refunds; each is part of the public API contract. */
public enum FeeErrorCode implements ErrorCode {

	INVOICE_HAS_PAYMENTS(409, "Payments stand against this invoice, so it cannot be cancelled."), //
	INVOICE_EXISTS_FOR_PERIOD(409, "The student already has an invoice for this period."), //
	FEE_ASSIGNMENT_TARGET_INVALID(400, "Assign the fee to exactly one student or classroom."), //
	FEE_ALREADY_ASSIGNED(409, "The fee structure is already assigned to this student or classroom."), //
	DISCOUNT_EXCEEDS_FEE(400, "The discounts would exceed the fee."), //
	NOTHING_OUTSTANDING(409, "Nothing is outstanding for this fee."), //
	ONLINE_PAYMENT_PENDING(409, "The online payment is not completed yet."), //
	SCHOOL_CURRENCY_MISSING(409, "The school has no currency configured."), //
	GATEWAY_PAYMENT_MANUAL(400, "Online payments are recorded by the payment gateway, not by hand."), //
	PAYMENT_HAS_REFUNDS(409, "The payment has refunds, so its cheque cannot be marked bounced."), //
	BOUNCED_CHEQUE_HAS_NO_RECEIPT(409, "A bounced cheque has no receipt."), //
	CHEQUE_BOUNCED(409, "The cheque bounced, so there is nothing to refund."), //
	REFUND_EXCEEDS_REFUNDABLE(400, "The refund exceeds the refundable amount."), //
	GATEWAY_REFUND_FAILED(502, "The payment gateway could not process the refund; nothing was refunded."), //
	INDIVIDUAL_AMOUNT_NOT_ALLOWED(400, "Only a student's own fee can carry an individual amount."), //
	AMOUNT_INVALID(400, "The amount must be positive with at most 2 decimals."), //
	DISCOUNT_PERCENT_TOO_HIGH(400, "A percentage discount cannot exceed 100."), //
	DISCOUNT_ALREADY_REVOKED(409, "The discount is already revoked."), //
	INSTALLMENT_SHARE_INVALID(400, "An installment's share must be between 0.01% and 100%."), //
	NOTHING_DUE(409, "Nothing is due from this student in the period."), //
	INVOICE_DUE_BEFORE_ISSUE(400, "An invoice cannot fall due before it is issued."), //
	INVOICE_ALREADY_CANCELLED(409, "The invoice is already cancelled."), //
	INVOICE_FEE_MISMATCH(400, "The invoice does not bill this fee to this student."), //
	NOT_A_CHEQUE(400, "Only a cheque payment can bounce."), //
	RECEIPT_EXISTS(409, "The payment already has a receipt."), //
	REFUND_NOT_PENDING(409, "Only a pending refund can be settled."), //
	INVOICE_PERIOD_INVALID(400, "An invoice needs a start, end and due date, and must not end before it starts."), //
	PAYMENT_MODE_REQUIRED(400, "A payment needs a payment mode."), //
	CHEQUE_DETAILS_REQUIRED(400, "A cheque payment needs the cheque number, date and bank."), //
	TRANSACTION_REFERENCE_REQUIRED(400, "This payment mode needs a transaction reference."), //
	INSTALLMENT_PLAN_INVALID(400, "The installment plan is not valid; see the message.");

	private final int httpStatus;
	private final String description;

	FeeErrorCode(int httpStatus, String description) {
		this.httpStatus = httpStatus;
		this.description = description;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public int httpStatus() {
		return httpStatus;
	}

	@Override
	public String description() {
		return description;
	}
}

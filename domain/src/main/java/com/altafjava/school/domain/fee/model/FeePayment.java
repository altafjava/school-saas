package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_payments")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class FeePayment extends SoftDeletableEntity {

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "paid_at", nullable = false)
	private LocalDateTime paidAt;

	@Column(name = "receipt_number", nullable = false, length = 100)
	private String receiptNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_source", nullable = false, length = 20)
	@Builder.Default
	private PaymentSource paymentSource = PaymentSource.MANUAL;

	// Set iff paymentSource == GATEWAY — unique per tenant (see uq_fee_payments_tenant_gateway_ref),
	// null for MANUAL payments so the uniqueness constraint never applies to them.
	@Column(name = "gateway_charge_reference", length = 255)
	private String gatewayChargeReference;

	// Set iff paymentSource == GATEWAY — the PaymentGatewayType this charge was made through.
	@Column(name = "gateway_provider_type", length = 50)
	private String gatewayProviderType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_mode", nullable = false, length = 20)
	@Builder.Default
	private PaymentMode paymentMode = PaymentMode.CASH;

	// Set for UPI, bank transfer and card payments.
	@Column(name = "transaction_reference", length = 100)
	private String transactionReference;

	@Column(name = "cheque_number", length = 50)
	private String chequeNumber;

	@Column(name = "cheque_date")
	private LocalDate chequeDate;

	@Column(name = "cheque_bank", length = 100)
	private String chequeBank;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private FeePaymentStatus status = FeePaymentStatus.CLEARED;

	@Column(name = "bounced_at")
	private LocalDateTime bouncedAt;

	@Column(name = "bounce_reason", length = 500)
	private String bounceReason;

	// The issued receipt document, set the first time a receipt is downloaded.
	@Column(name = "receipt_document_id")
	private Long receiptDocumentId;

	public static FeePayment create(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber, PaymentInstrument instrument) {
		return FeePayment.builder()
				.studentId(studentId)
				.feeStructureId(feeStructureId)
				.paidAmount(paidAmount)
				.paidAt(paidAt)
				.receiptNumber(receiptNumber)
				.paymentMode(instrument.mode())
				.transactionReference(instrument.transactionReference())
				.chequeNumber(instrument.chequeNumber())
				.chequeDate(instrument.chequeDate())
				.chequeBank(instrument.chequeBank())
				.build();
	}

	/** Keeps the row as evidence but takes the money out of every balance and total. */
	public void bounce(String reason, LocalDateTime bouncedAt) {
		if (paymentMode != PaymentMode.CHEQUE) {
			throw new BusinessException("Only a cheque payment can bounce, this one is " + paymentMode);
		}
		if (status == FeePaymentStatus.BOUNCED) {
			throw new BusinessException("This cheque has already bounced");
		}
		this.status = FeePaymentStatus.BOUNCED;
		this.bouncedAt = bouncedAt;
		this.bounceReason = reason;
	}

	public void attachReceipt(Long receiptDocumentId) {
		if (this.receiptDocumentId != null) {
			throw new BusinessException("This payment already has a receipt");
		}
		this.receiptDocumentId = receiptDocumentId;
	}

	public boolean isBounced() {
		return status == FeePaymentStatus.BOUNCED;
	}

	public static FeePayment recordFromGateway(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber, String gatewayProviderType, String gatewayChargeReference) {
		return FeePayment.builder()
				.studentId(studentId)
				.feeStructureId(feeStructureId)
				.paidAmount(paidAmount)
				.paidAt(paidAt)
				.receiptNumber(receiptNumber)
				.paymentSource(PaymentSource.GATEWAY)
				.paymentMode(PaymentMode.GATEWAY)
				.gatewayProviderType(gatewayProviderType)
				.gatewayChargeReference(gatewayChargeReference)
				.build();
	}
}

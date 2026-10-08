package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
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

	public static FeePayment create(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber) {
		return FeePayment.builder()
				.studentId(studentId)
				.feeStructureId(feeStructureId)
				.paidAmount(paidAmount)
				.paidAt(paidAt)
				.receiptNumber(receiptNumber)
				.build();
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
				.gatewayProviderType(gatewayProviderType)
				.gatewayChargeReference(gatewayChargeReference)
				.build();
	}
}

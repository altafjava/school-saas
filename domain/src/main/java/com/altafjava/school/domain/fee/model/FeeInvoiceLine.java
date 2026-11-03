package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One charge on an invoice: a whole fee, or one installment of it when {@code installmentNumber} is set. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeInvoiceLine {

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Column(name = "installment_number")
	private Integer installmentNumber;

	@Column(name = "description", nullable = false, length = 300)
	private String description;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	public FeeInvoiceLine(Long feeStructureId, Integer installmentNumber, String description, BigDecimal amount) {
		this.feeStructureId = feeStructureId;
		this.installmentNumber = installmentNumber;
		this.description = description;
		this.amount = amount;
	}

	/** The charge a line bills; an invoice never bills the same charge twice. */
	public ChargeKey charge() {
		return new ChargeKey(feeStructureId, installmentNumber);
	}

	public record ChargeKey(Long feeStructureId, Integer installmentNumber) {
	}
}

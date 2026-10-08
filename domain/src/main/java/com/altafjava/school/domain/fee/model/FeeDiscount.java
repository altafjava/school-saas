package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A reduction of one student's fee for one {@link FeeStructure} — a scholarship, sibling or
 * staff-ward concession, hardship waiver. {@code category} is open text so a school names its own
 * concession kinds. Granting and revoking are the only operations: a discount is never edited, so
 * the record of what was granted, by whom and why stays intact; revoking ends its effect.
 * A {@code PERCENTAGE} applies to the structure's gross amount, never compounding with other
 * discounts.
 */
@Entity
@Table(name = "fee_discounts")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeDiscount extends SoftDeletableEntity {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	// FK to students.id
	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// FK to fee_structures.id
	@Column(name = "fee_structure_id", nullable = false)
	private Long feeStructureId;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 20)
	private DiscountType discountType;

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "category", nullable = false, length = 50)
	private String category;

	@Column(name = "reason", nullable = false, length = 500)
	private String reason;

	// FK to platform users.id — who granted it.
	@Column(name = "granted_by_user_id")
	private Long grantedByUserId;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "revocation_reason", length = 500)
	private String revocationReason;

	public static FeeDiscount grant(Long studentId, Long feeStructureId, DiscountType discountType,
			BigDecimal discountValue, String category, String reason, Long grantedByUserId) {
		if (discountValue == null || discountValue.signum() <= 0) {
			throw new BusinessException("Discount value must be greater than zero");
		}
		if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(ONE_HUNDRED) > 0) {
			throw new BusinessException("A percentage discount cannot exceed 100");
		}
		return FeeDiscount.builder()
				.studentId(studentId)
				.feeStructureId(feeStructureId)
				.discountType(discountType)
				.discountValue(discountValue)
				.category(category)
				.reason(reason)
				.grantedByUserId(grantedByUserId)
				.build();
	}

	public boolean isActive() {
		return revokedAt == null;
	}

	public void revoke(String reason) {
		if (!isActive()) {
			throw new BusinessException("Discount is already revoked");
		}
		this.revokedAt = Instant.now();
		this.revocationReason = reason;
	}

	/** The amount this discount takes off a structure whose gross amount is {@code gross}. */
	public BigDecimal amountOn(BigDecimal gross) {
		BigDecimal amount = discountType == DiscountType.PERCENTAGE
				? gross.multiply(discountValue).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
				: discountValue;
		return amount.min(gross);
	}
}

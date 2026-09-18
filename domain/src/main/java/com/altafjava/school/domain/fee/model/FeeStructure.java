package com.altafjava.school.domain.fee.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_structures")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class FeeStructure extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "frequency", nullable = false, length = 20)
	private FeeFrequency frequency;

	@Column(name = "plan_type", length = 100)
	private String planType;

	// Tenant-wide defaults for every FeeAssignment of this structure that doesn't set its own
	// override — see FeeBalanceCalculator's assignment-then-structure-then-hardcoded fallback
	// chain, mirroring AlertRule's per-rule-value-with-default-fallback pattern.
	@Column(name = "grace_days")
	private Integer graceDays;

	@Column(name = "late_fee_percentage", precision = 5, scale = 2)
	private BigDecimal lateFeePercentage;

	public static FeeStructure create(String name, BigDecimal amount, FeeFrequency frequency, String planType) {
		return FeeStructure.builder()
				.name(name)
				.amount(amount)
				.frequency(frequency)
				.planType(planType)
				.build();
	}

	public void reviseAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void configureLateFeePolicy(Integer graceDays, BigDecimal lateFeePercentage) {
		this.graceDays = graceDays;
		this.lateFeePercentage = lateFeePercentage;
	}
}

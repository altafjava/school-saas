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
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "fee_structures")
@SQLRestriction("deleted = false")
@Getter
@Setter
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

	public static FeeStructure create(String name, BigDecimal amount, FeeFrequency frequency, String planType) {
		return FeeStructure.builder()
				.name(name)
				.amount(amount)
				.frequency(frequency)
				.planType(planType)
				.build();
	}
}

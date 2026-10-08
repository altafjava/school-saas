package com.altafjava.school.domain.payroll.model;

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

/**
 * Tenant-defined salary pay component — a runtime catalog rather than fixed columns, mirroring
 * {@code LeaveType}, since compensation structure varies widely by region.
 */
@Entity
@Table(name = "pay_component_definitions")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PayComponentDefinition extends SoftDeletableEntity {

	@Column(name = "code", nullable = false, length = 50)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private PayComponentType type;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static PayComponentDefinition create(String code, String name, PayComponentType type, int displayOrder) {
		return PayComponentDefinition.builder()
				.code(code)
				.name(name)
				.type(type)
				.displayOrder(displayOrder)
				.active(true)
				.build();
	}

	public void update(String name, boolean active, int displayOrder) {
		this.name = name;
		this.active = active;
		this.displayOrder = displayOrder;
	}
}

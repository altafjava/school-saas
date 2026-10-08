package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.school.domain.payroll.converter.PayComponentAmountListConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A employee's compensation terms as of {@link #effectiveFrom}. At most one structure is
 * {@link #active} per employee at a time — {@code SalaryStructureService} deactivates the previous
 * one when a new one is created, mirroring how {@code Term}/{@code AcademicYear} flip {@code current}.
 *
 * <p>
 * Pay components are a tenant-defined list ({@link PayComponentDefinition}), not fixed columns —
 * compensation structure varies materially by region. Does not compute statutory deductions.
 */
@Entity
@Table(name = "salary_structures")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SalaryStructure extends SoftDeletableEntity {

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Convert(converter = PayComponentAmountListConverter.class)
	@Column(name = "components_json", nullable = false)
	private List<PayComponentAmount> components;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static SalaryStructure create(Long employeeId, List<PayComponentAmount> components,
			LocalDate effectiveFrom) {
		if (components == null || components.isEmpty()) {
			throw new BusinessException("At least one pay component is required");
		}
		SalarySnapshot snapshot = new SalarySnapshot(components);
		if (snapshot.grossPay().signum() <= 0) {
			throw new BusinessException("Total earning components must be greater than zero");
		}
		return SalaryStructure.builder()
				.employeeId(employeeId)
				.components(components)
				.effectiveFrom(effectiveFrom)
				.active(true)
				.build();
	}

	public void deactivate() {
		this.active = false;
	}

	public BigDecimal grossPay() {
		return toSnapshot().grossPay();
	}

	public SalarySnapshot toSnapshot() {
		return new SalarySnapshot(components);
	}
}

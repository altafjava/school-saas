package com.altafjava.school.domain.payroll.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * The pay-component line items copied out of a {@link SalaryStructure} at payslip generation
 * time — a distinct type (rather than passing the structure itself into {@code Payslip}) so a later
 * revision to the employee's structure can never retroactively change an already-generated payslip.
 */
public record SalarySnapshot(List<PayComponentAmount> components) {

	public SalarySnapshot {
		components = List.copyOf(components);
	}

	public BigDecimal grossPay() {
		return sum(PayComponentType.EARNING);
	}

	public BigDecimal totalDeductions() {
		return sum(PayComponentType.DEDUCTION);
	}

	private BigDecimal sum(PayComponentType type) {
		return components.stream()
				.filter(component -> component.type() == type)
				.map(PayComponentAmount::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}

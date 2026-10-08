package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Tenant-defined leave category (e.g. Sick, Casual, Earned) — schools vary widely here, so this is
 * a runtime catalog rather than a hardcoded enum, mirroring {@code Department}.
 */
@Entity
@Table(name = "leave_types")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveType extends SoftDeletableEntity {

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "default_annual_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal defaultAnnualDays;

	@Column(name = "active", nullable = false)
	private boolean active;

	// Whether leave under this type is compensated. Payroll's loss-of-pay calculation is the only
	// consumer today — everything created before this column existed defaults to paid, matching the
	// prior (implicit) behavior where all leave was treated as compensated.
	@Column(name = "paid", nullable = false)
	private boolean paid;

	// Whether a probationary employee may request this leave type — defaults true so existing leave
	// types keep their prior (implicit) behavior of being available to everyone.
	@Column(name = "available_during_probation", nullable = false)
	private boolean availableDuringProbation;

	@Column(name = "carry_forward_enabled", nullable = false)
	private boolean carryForwardEnabled;

	// Cap on days carried into the next academic year — null means "unlimited" when carry-forward
	// is enabled.
	@Column(name = "max_carry_forward_days", precision = 5, scale = 1)
	private BigDecimal maxCarryForwardDays;

	// Carried-forward days not used within this many months of the new academic year's start are
	// forfeited (see LeaveCarryForwardExpiryJob) — null means carried-forward days never expire.
	@Column(name = "carry_forward_expiry_months")
	private Integer carryForwardExpiryMonths;

	public static LeaveType create(String name, BigDecimal defaultAnnualDays) {
		return LeaveType.builder()
				.name(name)
				.defaultAnnualDays(defaultAnnualDays)
				.active(true)
				.paid(true)
				.availableDuringProbation(true)
				.carryForwardEnabled(false)
				.build();
	}

	public void updateDetails(String name, BigDecimal defaultAnnualDays) {
		this.name = name;
		this.defaultAnnualDays = defaultAnnualDays;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	public void markUnpaid() {
		this.paid = false;
	}

	public void markPaid() {
		this.paid = true;
	}

	public void restrictDuringProbation() {
		this.availableDuringProbation = false;
	}

	public void allowDuringProbation() {
		this.availableDuringProbation = true;
	}

	public void configureCarryForward(boolean enabled, BigDecimal maxCarryForwardDays,
			Integer carryForwardExpiryMonths) {
		this.carryForwardEnabled = enabled;
		this.maxCarryForwardDays = enabled ? maxCarryForwardDays : null;
		this.carryForwardExpiryMonths = enabled ? carryForwardExpiryMonths : null;
	}
}

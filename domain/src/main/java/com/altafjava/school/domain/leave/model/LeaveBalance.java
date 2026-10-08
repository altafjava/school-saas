package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "leave_balances")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveBalance extends SoftDeletableEntity {

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(name = "leave_type_id", nullable = false)
	private Long leaveTypeId;

	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "allocated_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal allocatedDays;

	@Column(name = "used_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal usedDays;

	// How much of allocatedDays came from the previous academic year's carry-forward, and when
	// that portion is forfeited if unused — see LeaveCarryForwardExpiryJob.
	@Column(name = "carried_forward_days", nullable = false, precision = 5, scale = 1)
	private BigDecimal carriedForwardDays;

	@Column(name = "carry_forward_expires_at")
	private LocalDate carryForwardExpiresAt;

	public static LeaveBalance allocate(Long employeeId, Long leaveTypeId, Long academicYearId,
			BigDecimal allocatedDays) {
		return LeaveBalance.builder()
				.employeeId(employeeId)
				.leaveTypeId(leaveTypeId)
				.academicYearId(academicYearId)
				.allocatedDays(allocatedDays)
				.usedDays(BigDecimal.ZERO)
				.carriedForwardDays(BigDecimal.ZERO)
				.build();
	}

	public BigDecimal remainingDays() {
		return allocatedDays.subtract(usedDays);
	}

	public void deduct(BigDecimal days) {
		if (days.compareTo(remainingDays()) > 0) {
			throw new BusinessException("Insufficient leave balance: requested " + days + ", remaining "
					+ remainingDays());
		}
		this.usedDays = this.usedDays.add(days);
	}

	public void credit(BigDecimal days) {
		BigDecimal reversed = this.usedDays.subtract(days);
		this.usedDays = reversed.max(BigDecimal.ZERO);
	}

	// Adds a carry-forward amount on top of whatever this balance was already allocated (its
	// default annual days) — called once, right after LeaveBalance.allocate, when the previous
	// academic year's LeaveType had carryForwardEnabled and a remaining balance to bring forward.
	public void applyCarryForward(BigDecimal days, LocalDate expiresAt) {
		this.allocatedDays = this.allocatedDays.add(days);
		this.carriedForwardDays = days;
		this.carryForwardExpiresAt = expiresAt;
	}

	/**
	 * Forfeits whatever carried-forward days remain unused once their expiry date has passed —
	 * reduces {@code allocatedDays} back down by the unused portion and clears the carry-forward
	 * tracking fields, so it is a no-op if called again. Returns the number of days forfeited (zero
	 * if nothing was due to expire).
	 */
	public BigDecimal forfeitExpiredCarryForward(LocalDate today) {
		if (carryForwardExpiresAt == null || today.isBefore(carryForwardExpiresAt)) {
			return BigDecimal.ZERO;
		}
		BigDecimal forfeited = carriedForwardDays.min(remainingDays()).max(BigDecimal.ZERO);
		this.allocatedDays = this.allocatedDays.subtract(forfeited);
		this.carriedForwardDays = BigDecimal.ZERO;
		this.carryForwardExpiresAt = null;
		return forfeited;
	}
}

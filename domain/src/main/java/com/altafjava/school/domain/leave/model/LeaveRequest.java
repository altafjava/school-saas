package com.altafjava.school.domain.leave.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
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
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "leave_requests")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LeaveRequest extends SoftDeletableEntity {

	@Column(name = "employee_id", nullable = false)
	private Long employeeId;

	@Column(name = "leave_type_id", nullable = false)
	private Long leaveTypeId;

	// Captured at submit time rather than re-resolved at approval time, so an academic-year
	// rollover between submission and approval cannot silently point the deduction at the wrong
	// year's balance.
	@Column(name = "academic_year_id", nullable = false)
	private Long academicYearId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "days_requested", nullable = false, precision = 5, scale = 1)
	private BigDecimal daysRequested;

	@Column(name = "reason", length = 500)
	private String reason;

	@Column(name = "approved_by_user_id")
	private Long approvedByUserId;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	// Frozen at submission from the leave type, so a later policy change never re-routes a request
	// already in flight.
	@Column(name = "approvals_required", nullable = false)
	private int approvalsRequired;

	@Column(name = "approvals_granted", nullable = false)
	private int approvalsGranted;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LeaveRequestStatus status;

	// daysRequested is precomputed by the caller (LeaveDayCalculator, given the tenant's holiday
	// calendar) rather than derived here — this entity has no way to reach holiday data itself.
	public static LeaveRequest submit(Long employeeId, Long leaveTypeId, Long academicYearId, LocalDate startDate,
			LocalDate endDate, String reason, BigDecimal daysRequested, int approvalsRequired) {
		if (endDate.isBefore(startDate)) {
			throw new BusinessException("Leave end date cannot be before the start date");
		}
		return LeaveRequest.builder()
				.employeeId(employeeId)
				.leaveTypeId(leaveTypeId)
				.academicYearId(academicYearId)
				.startDate(startDate)
				.endDate(endDate)
				.daysRequested(daysRequested)
				.reason(reason)
				.status(LeaveRequestStatus.PENDING)
				.approvalsRequired(approvalsRequired)
				.build();
	}

	/** The approval this request is waiting on, or empty once it is decided or cancelled. */
	public Optional<LeaveApprovalStage> awaitingStage() {
		if (status != LeaveRequestStatus.PENDING) {
			return Optional.empty();
		}
		return Optional.of(approvalsRequired - approvalsGranted > 1
				? LeaveApprovalStage.DEPARTMENT_HEAD
				: LeaveApprovalStage.ADMINISTRATOR);
	}

	/** Records one approval; the request is approved once every required level has given theirs. */
	public void approve(Long approvedByUserId) {
		requireStatus(LeaveRequestStatus.PENDING, "approve");
		this.approvalsGranted++;
		if (this.approvalsGranted >= this.approvalsRequired) {
			this.status = LeaveRequestStatus.APPROVED;
			this.approvedByUserId = approvedByUserId;
			this.approvedAt = LocalDateTime.now();
		}
	}

	public void reject(Long rejectedByUserId, String rejectionReason) {
		requireStatus(LeaveRequestStatus.PENDING, "reject");
		this.status = LeaveRequestStatus.REJECTED;
		this.approvedByUserId = rejectedByUserId;
		this.approvedAt = LocalDateTime.now();
		this.rejectionReason = rejectionReason;
	}

	public void cancel() {
		if (this.status == LeaveRequestStatus.REJECTED || this.status == LeaveRequestStatus.CANCELLED) {
			throw new BusinessException("Cannot cancel a leave request that is already " + this.status);
		}
		if (this.startDate.isBefore(LocalDate.now())) {
			throw new BusinessException("Cannot cancel a leave request that has already started");
		}
		this.status = LeaveRequestStatus.CANCELLED;
	}

	private void requireStatus(LeaveRequestStatus required, String action) {
		if (this.status != required) {
			throw new BusinessException("Cannot " + action + " a leave request in status " + this.status);
		}
	}
}

package com.altafjava.school.domain.visitor.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A visit awaiting, or granted, the host's go-ahead: pre-registered ahead of the day, or raised at
 * the front desk for a walk-in. A visitor can only be checked in against an APPROVED request for
 * today.
 */
@Entity
@Table(name = "visitor_requests")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class VisitorRequest extends SoftDeletableEntity {

	@Pii
	@Column(name = "visitor_name", nullable = false, length = 150)
	private String visitorName;

	@Pii
	@Column(name = "visitor_phone", length = 50)
	private String visitorPhone;

	@Column(name = "purpose", nullable = false, length = 500)
	private String purpose;

	// FK to employees.id — anyone on staff can host, not only teachers.
	@Column(name = "host_employee_id", nullable = false)
	private Long hostEmployeeId;

	@Column(name = "visit_date", nullable = false)
	private LocalDate visitDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 20)
	private VisitorRequestSource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private VisitorRequestStatus status;

	// FK to platform file_metadata.public_id — see Student.photoFilePublicId.
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	@Column(name = "requested_by_user_id")
	private Long requestedByUserId;

	@Column(name = "decided_by_user_id")
	private Long decidedByUserId;

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Column(name = "decision_reason", length = 500)
	private String decisionReason;

	public static VisitorRequest preRegister(String visitorName, String visitorPhone, String purpose,
			Long hostEmployeeId, LocalDate visitDate, Long requestedByUserId, LocalDate today) {
		if (visitDate.isBefore(today)) {
			throw new BusinessException("A visit cannot be pre-registered for a date that has passed");
		}
		return request(visitorName, visitorPhone, purpose, hostEmployeeId, visitDate,
				VisitorRequestSource.PRE_REGISTERED, requestedByUserId);
	}

	public static VisitorRequest walkIn(String visitorName, String visitorPhone, String purpose,
			Long hostEmployeeId, Long requestedByUserId, LocalDate today) {
		return request(visitorName, visitorPhone, purpose, hostEmployeeId, today, VisitorRequestSource.WALK_IN,
				requestedByUserId);
	}

	private static VisitorRequest request(String visitorName, String visitorPhone, String purpose,
			Long hostEmployeeId, LocalDate visitDate, VisitorRequestSource source, Long requestedByUserId) {
		return VisitorRequest.builder()
				.visitorName(visitorName)
				.visitorPhone(visitorPhone)
				.purpose(purpose)
				.hostEmployeeId(hostEmployeeId)
				.visitDate(visitDate)
				.source(source)
				.status(VisitorRequestStatus.PENDING)
				.requestedByUserId(requestedByUserId)
				.build();
	}

	public void attachPhoto(UUID photoFilePublicId) {
		if (status == VisitorRequestStatus.CHECKED_IN || status == VisitorRequestStatus.REJECTED
				|| status == VisitorRequestStatus.CANCELLED) {
			throw new BusinessException("A photo cannot be added to a request that is " + status);
		}
		this.photoFilePublicId = photoFilePublicId;
	}

	public void approve(Long decidedByUserId, LocalDate today) {
		requireStatus(VisitorRequestStatus.PENDING, "approve");
		if (visitDate.isBefore(today)) {
			throw new BusinessException("The visit date " + visitDate + " has passed");
		}
		decide(VisitorRequestStatus.APPROVED, decidedByUserId, null);
	}

	public void reject(Long decidedByUserId, String reason) {
		requireStatus(VisitorRequestStatus.PENDING, "reject");
		decide(VisitorRequestStatus.REJECTED, decidedByUserId, reason);
	}

	public void cancel() {
		if (status != VisitorRequestStatus.PENDING && status != VisitorRequestStatus.APPROVED) {
			throw new BusinessException("Cannot cancel a visitor request that is " + status);
		}
		this.status = VisitorRequestStatus.CANCELLED;
	}

	/** Whether the visitor may be let in now: approved, and for today's date. */
	public void requireAdmissibleOn(LocalDate today) {
		if (status != VisitorRequestStatus.APPROVED) {
			throw new BusinessException("The visit is not approved (status " + status + ")");
		}
		if (!visitDate.equals(today)) {
			throw new BusinessException("The visit is approved for " + visitDate + ", not " + today);
		}
	}

	public void markCheckedIn() {
		requireStatus(VisitorRequestStatus.APPROVED, "check in");
		this.status = VisitorRequestStatus.CHECKED_IN;
	}

	private void decide(VisitorRequestStatus decision, Long decidedByUserId, String reason) {
		this.status = decision;
		this.decidedByUserId = decidedByUserId;
		this.decidedAt = Instant.now();
		this.decisionReason = reason;
	}

	private void requireStatus(VisitorRequestStatus required, String action) {
		if (status != required) {
			throw new BusinessException("Cannot " + action + " a visitor request that is " + status);
		}
	}
}

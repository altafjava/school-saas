package com.altafjava.school.domain.helpdesk.model;

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

/**
 * {@code raisedByUserId} is a plain platform {@code User} id, not a {@code Teacher}/{@code Student}
 * reference — unlike {@code VisitorLog.hostTeacherId}, a helpdesk ticket is meant to be raisable by
 * the whole tenant population (teacher, parent, student, admin), and there is no single school-saas
 * staff/actor entity that covers all of them. {@code category} is a fixed enum rather than a
 * tenant-configurable catalog (unlike {@code LeaveType}/{@code Department}) — support-ticket
 * categories are a small, generic, cross-tenant taxonomy every school needs the same handful of
 * buckets for, so a runtime catalog table would add configuration surface without a real per-tenant
 * customization need (mirrors {@code DisciplineIncident.severity}).
 */
@Entity
@Table(name = "tickets")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Ticket extends SoftDeletableEntity {

	@Column(name = "raised_by_user_id", nullable = false)
	private Long raisedByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 20)
	private TicketCategory category;

	@Column(name = "subject", nullable = false, length = 200)
	private String subject;

	@Column(name = "description", nullable = false, length = 2000)
	private String description;

	@Column(name = "assigned_to_user_id")
	private Long assignedToUserId;

	@Column(name = "resolution", length = 2000)
	private String resolution;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TicketStatus status;

	public static Ticket raise(Long raisedByUserId, TicketCategory category, String subject, String description) {
		return Ticket.builder()
				.raisedByUserId(raisedByUserId)
				.category(category)
				.subject(subject)
				.description(description)
				.status(TicketStatus.OPEN)
				.build();
	}

	public void assign(Long assignedToUserId) {
		if (this.status == TicketStatus.CLOSED) {
			throw new BusinessException("Cannot assign a closed ticket");
		}
		this.assignedToUserId = assignedToUserId;
		this.status = TicketStatus.IN_PROGRESS;
	}

	public void resolve(String resolutionText) {
		if (this.status != TicketStatus.OPEN && this.status != TicketStatus.IN_PROGRESS) {
			throw new BusinessException("Cannot resolve a ticket in status " + this.status);
		}
		this.status = TicketStatus.RESOLVED;
		this.resolution = resolutionText;
	}

	public void close() {
		if (this.status != TicketStatus.RESOLVED) {
			throw new BusinessException("Cannot close a ticket that is not resolved (current status: "
					+ this.status + ")");
		}
		this.status = TicketStatus.CLOSED;
	}

	public void reopen() {
		if (this.status != TicketStatus.RESOLVED && this.status != TicketStatus.CLOSED) {
			throw new BusinessException("Cannot reopen a ticket in status " + this.status);
		}
		this.status = TicketStatus.OPEN;
		this.resolution = null;
	}
}

package com.altafjava.school.domain.leave.model;

import java.time.Instant;
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

/** Append-only record of one approver's decision on a {@link LeaveRequest}. */
@Entity
@Table(name = "leave_request_approvals")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class LeaveApproval extends SoftDeletableEntity {

	// FK to leave_requests.id
	@Column(name = "leave_request_id", nullable = false)
	private Long leaveRequestId;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision", nullable = false, length = 20)
	private LeaveApprovalDecision decision;

	// FK to platform users.id
	@Column(name = "decided_by_user_id", nullable = false)
	private Long decidedByUserId;

	@Column(name = "decided_at", nullable = false)
	private Instant decidedAt;

	@Column(name = "remarks", length = 500)
	private String remarks;

	@Enumerated(EnumType.STRING)
	@Column(name = "stage", nullable = false, length = 30)
	private LeaveApprovalStage stage;

	public static LeaveApproval approved(Long leaveRequestId, LeaveApprovalStage stage, Long decidedByUserId) {
		return decision(leaveRequestId, stage, LeaveApprovalDecision.APPROVED, decidedByUserId, null);
	}

	public static LeaveApproval rejected(Long leaveRequestId, LeaveApprovalStage stage, Long decidedByUserId,
			String remarks) {
		return decision(leaveRequestId, stage, LeaveApprovalDecision.REJECTED, decidedByUserId, remarks);
	}

	private static LeaveApproval decision(Long leaveRequestId, LeaveApprovalStage stage,
			LeaveApprovalDecision decision, Long decidedByUserId, String remarks) {
		return LeaveApproval.builder()
				.leaveRequestId(leaveRequestId)
				.stage(stage)
				.decision(decision)
				.decidedByUserId(decidedByUserId)
				.decidedAt(Instant.now())
				.remarks(remarks)
				.build();
	}
}

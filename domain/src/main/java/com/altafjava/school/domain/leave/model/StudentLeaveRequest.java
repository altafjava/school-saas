package com.altafjava.school.domain.leave.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
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
import lombok.experimental.SuperBuilder;

/** A leave of absence for a student, raised by a guardian or staff and decided by the class teacher. */
@Entity
@Table(name = "student_leave_requests")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class StudentLeaveRequest extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	// Frozen at submission: a later class move must not change which register the leave excuses.
	@Column(name = "classroom_id", nullable = false)
	private Long classroomId;

	@Column(name = "requested_by_user_id", nullable = false)
	private Long requestedByUserId;

	@Column(name = "decided_by_user_id")
	private Long decidedByUserId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "reason", nullable = false, length = 500)
	private String reason;

	// Uploaded through /api/v1/files first, as with Student.photoFilePublicId.
	@Column(name = "attachment_file_public_id")
	private UUID attachmentFilePublicId;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LeaveRequestStatus status;

	public static StudentLeaveRequest submit(Long studentId, Long classroomId, Long requestedByUserId,
			LocalDate startDate, LocalDate endDate, String reason, UUID attachmentFilePublicId) {
		if (endDate.isBefore(startDate)) {
			throw new BusinessException("Leave end date cannot be before the start date");
		}
		return StudentLeaveRequest.builder()
				.studentId(studentId)
				.classroomId(classroomId)
				.requestedByUserId(requestedByUserId)
				.startDate(startDate)
				.endDate(endDate)
				.reason(reason)
				.attachmentFilePublicId(attachmentFilePublicId)
				.status(LeaveRequestStatus.PENDING)
				.build();
	}

	public void approve(Long decidedByUserId) {
		requirePending("approve");
		this.status = LeaveRequestStatus.APPROVED;
		this.decidedByUserId = decidedByUserId;
		this.decidedAt = LocalDateTime.now();
	}

	public void reject(Long decidedByUserId, String rejectionReason) {
		requirePending("reject");
		this.status = LeaveRequestStatus.REJECTED;
		this.decidedByUserId = decidedByUserId;
		this.decidedAt = LocalDateTime.now();
		this.rejectionReason = rejectionReason;
	}

	public void cancel() {
		requirePending("cancel");
		this.status = LeaveRequestStatus.CANCELLED;
	}

	private void requirePending(String action) {
		if (status != LeaveRequestStatus.PENDING) {
			throw new BusinessException("Cannot " + action + " a leave request that is " + status);
		}
	}
}

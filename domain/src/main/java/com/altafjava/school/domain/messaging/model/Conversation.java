package com.altafjava.school.domain.messaging.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.model.TenantEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A private thread between one guardian and one teacher about one child. */
@Entity
@Table(name = "conversations")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Conversation extends TenantEntity {

	@Column(name = "guardian_id", nullable = false, updatable = false)
	private Long guardianId;

	@Column(name = "teacher_id", nullable = false, updatable = false)
	private Long teacherId;

	@Column(name = "student_id", nullable = false, updatable = false)
	private Long studentId;

	@Column(name = "guardian_user_id", nullable = false, updatable = false)
	private Long guardianUserId;

	@Column(name = "teacher_user_id", nullable = false, updatable = false)
	private Long teacherUserId;

	@Column(name = "guardian_last_read_message_id", nullable = false)
	private long guardianLastReadMessageId;

	@Column(name = "teacher_last_read_message_id", nullable = false)
	private long teacherLastReadMessageId;

	@Column(name = "last_message_at")
	private Instant lastMessageAt;

	public static Conversation open(Long guardianId, Long guardianUserId, Long teacherId, Long teacherUserId,
			Long studentId) {
		return Conversation.builder()
				.guardianId(guardianId)
				.guardianUserId(guardianUserId)
				.teacherId(teacherId)
				.teacherUserId(teacherUserId)
				.studentId(studentId)
				.build();
	}

	public boolean hasParticipant(Long userId) {
		return guardianUserId.equals(userId) || teacherUserId.equals(userId);
	}

	public Long otherParticipant(Long userId) {
		return guardianUserId.equals(userId) ? teacherUserId : guardianUserId;
	}

	public void markReadUpTo(Long userId, long messageId) {
		if (guardianUserId.equals(userId)) {
			this.guardianLastReadMessageId = Math.max(guardianLastReadMessageId, messageId);
		} else if (teacherUserId.equals(userId)) {
			this.teacherLastReadMessageId = Math.max(teacherLastReadMessageId, messageId);
		} else {
			throw new BusinessException(PlatformErrorCode.FORBIDDEN, "Only a participant has a read position");
		}
	}

	public long lastReadMessageId(Long userId) {
		return guardianUserId.equals(userId) ? guardianLastReadMessageId : teacherLastReadMessageId;
	}
}

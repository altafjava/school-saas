package com.altafjava.school.domain.messaging.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.TenantEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A participant asking the school's reviewers to look at one message. */
@Entity
@Table(name = "message_reports")
@Getter
@SuperBuilder
@NoArgsConstructor
public class MessageReport extends TenantEntity {

	@Column(name = "message_id", nullable = false, updatable = false)
	private Long messageId;

	@Column(name = "conversation_id", nullable = false, updatable = false)
	private Long conversationId;

	@Column(name = "reported_by_user_id", nullable = false, updatable = false)
	private Long reportedByUserId;

	@Column(name = "resolved_by_user_id")
	private Long resolvedByUserId;

	@Pii
	@Column(name = "reason", length = 1000)
	private String reason;

	@Column(name = "resolution_note", length = 500)
	private String resolutionNote;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private MessageReportStatus status = MessageReportStatus.OPEN;

	public static MessageReport file(ConversationMessage message, Long reportedByUserId, String reason) {
		return MessageReport.builder()
				.messageId(message.getId())
				.conversationId(message.getConversationId())
				.reportedByUserId(reportedByUserId)
				.reason(reason)
				.build();
	}

	public void resolve(Long resolvedByUserId, String note, Instant now) {
		if (status == MessageReportStatus.RESOLVED) {
			throw new BusinessException("This report is already resolved");
		}
		this.status = MessageReportStatus.RESOLVED;
		this.resolvedByUserId = resolvedByUserId;
		this.resolutionNote = note;
		this.resolvedAt = now;
	}

	public void eraseReason() {
		this.reason = null;
	}
}

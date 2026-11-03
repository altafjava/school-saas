package com.altafjava.school.domain.messaging.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import com.altafjava.platform.core.model.TenantEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Immutable once sent, so a conversation under review shows what was really said. */
@Entity
@Table(name = "conversation_messages")
@Getter
@SuperBuilder
@NoArgsConstructor
public class ConversationMessage extends TenantEntity {

	@Column(name = "conversation_id", nullable = false, updatable = false)
	private Long conversationId;

	@Column(name = "sender_user_id", nullable = false, updatable = false)
	private Long senderUserId;

	// Null once erased; erasedAt tells a reader why it is empty.
	@Pii
	@Column(name = "body", columnDefinition = "TEXT")
	private String body;

	@Column(name = "erased_at")
	private Instant erasedAt;

	@ElementCollection(fetch = FetchType.LAZY)
	@BatchSize(size = 50)
	@CollectionTable(name = "conversation_message_attachments", joinColumns = @JoinColumn(name = "message_id"))
	@Column(name = "file_public_id", nullable = false)
	@Builder.Default
	private List<UUID> attachmentFilePublicIds = new ArrayList<>();

	public static ConversationMessage send(Long conversationId, Long senderUserId, String body,
			List<UUID> attachments) {
		return ConversationMessage.builder()
				.conversationId(conversationId)
				.senderUserId(senderUserId)
				.body(body)
				.attachmentFilePublicIds(new ArrayList<>(attachments))
				.build();
	}

	public void loadDetails() {
		Hibernate.initialize(attachmentFilePublicIds);
	}

	public void erase(Instant now) {
		this.body = null;
		this.attachmentFilePublicIds = new ArrayList<>();
		this.erasedAt = now;
	}
}

package com.altafjava.school.domain.messaging.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.messaging.model.ConversationMessage;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

	/** One conversation's unread count, as the id of the row and the number of unread messages. */
	record UnreadCount(Long conversationId, long unread) {
	}

	Optional<ConversationMessage> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	Page<ConversationMessage> findByConversationIdAndTenantIdOrderByIdDesc(Long conversationId, Long tenantId,
			Pageable pageable);

	Optional<ConversationMessage> findFirstByConversationIdAndTenantIdOrderByIdDesc(Long conversationId, Long tenantId);

	@Query("""
			SELECT COUNT(m) FROM ConversationMessage m, Conversation c
			WHERE m.conversationId = c.id AND m.tenantId = :tenantId AND c.tenantId = :tenantId
			  AND m.senderUserId <> :userId
			  AND ((c.guardianUserId = :userId AND m.id > c.guardianLastReadMessageId)
			    OR (c.teacherUserId = :userId AND m.id > c.teacherLastReadMessageId))
			""")
	long countUnread(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

	@Query("""
			SELECT new com.altafjava.school.domain.messaging.repository.ConversationMessageRepository$UnreadCount(
				c.id, COUNT(m))
			FROM ConversationMessage m, Conversation c
			WHERE m.conversationId = c.id AND m.tenantId = :tenantId AND c.tenantId = :tenantId
			  AND c.id IN :conversationIds AND m.senderUserId <> :userId
			  AND ((c.guardianUserId = :userId AND m.id > c.guardianLastReadMessageId)
			    OR (c.teacherUserId = :userId AND m.id > c.teacherLastReadMessageId))
			GROUP BY c.id
			""")
	List<UnreadCount> countUnreadByConversation(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
			@Param("conversationIds") Collection<Long> conversationIds);

	List<ConversationMessage> findAllByConversationIdAndTenantId(Long conversationId, Long tenantId);

	List<ConversationMessage> findAllBySenderUserIdAndTenantId(Long senderUserId, Long tenantId);
}

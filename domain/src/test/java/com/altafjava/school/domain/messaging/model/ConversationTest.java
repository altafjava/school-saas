package com.altafjava.school.domain.messaging.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class ConversationTest {

	private static final Long GUARDIAN_USER = 20L;
	private static final Long TEACHER_USER = 40L;

	private final Conversation conversation = Conversation.open(1L, GUARDIAN_USER, 3L, TEACHER_USER, 5L);

	@Test
	void onlyTheTwoParticipantsBelong() {
		assertTrue(conversation.hasParticipant(GUARDIAN_USER));
		assertTrue(conversation.hasParticipant(TEACHER_USER));
		assertFalse(conversation.hasParticipant(99L));
		assertEquals(TEACHER_USER, conversation.otherParticipant(GUARDIAN_USER));
	}

	@Test
	void readPosition_neverMovesBackwards_andIsPerParticipant() {
		conversation.markReadUpTo(GUARDIAN_USER, 10);
		conversation.markReadUpTo(GUARDIAN_USER, 4);

		assertEquals(10, conversation.lastReadMessageId(GUARDIAN_USER));
		assertEquals(0, conversation.lastReadMessageId(TEACHER_USER));
		assertThrows(BusinessException.class, () -> conversation.markReadUpTo(99L, 1));
	}

	@Test
	void erase_removesContentButKeepsTheRow() {
		ConversationMessage message = ConversationMessage.send(1L, GUARDIAN_USER, "Hello", List.of(UUID.randomUUID()));
		Instant now = Instant.now();

		message.erase(now);

		assertNull(message.getBody());
		assertTrue(message.getAttachmentFilePublicIds().isEmpty());
		assertEquals(now, message.getErasedAt());
	}

	@Test
	void report_resolvesOnce() {
		ConversationMessage message = ConversationMessage.send(1L, GUARDIAN_USER, "Hello", List.of());
		MessageReport report = MessageReport.file(message, TEACHER_USER, "Rude");

		report.resolve(7L, "Spoke to parent", Instant.now());

		assertEquals(MessageReportStatus.RESOLVED, report.getStatus());
		assertThrows(BusinessException.class, () -> report.resolve(7L, "Again", Instant.now()));
	}
}

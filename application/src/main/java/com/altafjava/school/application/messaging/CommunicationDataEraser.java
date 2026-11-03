package com.altafjava.school.application.messaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;
import com.altafjava.school.domain.announcement.repository.AnnouncementRecipientRepository;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.model.ConversationMessage;
import com.altafjava.school.domain.messaging.model.MessageReport;
import com.altafjava.school.domain.messaging.repository.ConversationMessageRepository;
import com.altafjava.school.domain.messaging.repository.ConversationRepository;
import com.altafjava.school.domain.messaging.repository.MessageReportRepository;
import lombok.RequiredArgsConstructor;

/**
 * Removes or hands over one person's messaging and announcement data for retention and data-subject
 * requests. A message's body goes but its row stays, so the other person's thread keeps its shape.
 */
@Component
@RequiredArgsConstructor
public class CommunicationDataEraser {

	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository messageRepository;
	private final MessageReportRepository reportRepository;
	private final AnnouncementRepository announcementRepository;
	private final AnnouncementRecipientRepository recipientRepository;
	private final NotificationService notificationService;

	public void eraseForUser(Long tenantId, Long userId) {
		Instant now = Instant.now();
		List<ConversationMessage> sent = messageRepository.findAllBySenderUserIdAndTenantId(userId, tenantId);
		sent.forEach(message -> message.erase(now));
		messageRepository.saveAll(sent);
		List<MessageReport> reports = reportRepository.findAllByReportedByUserIdAndTenantId(userId, tenantId);
		reports.forEach(MessageReport::eraseReason);
		reportRepository.saveAll(reports);
		recipientRepository.deleteByUser(tenantId, userId);
	}

	/** Threads about a child are about that child whoever wrote them, so both sides go. */
	public void eraseForStudent(Long tenantId, Long studentId) {
		conversationRepository.findAllByStudentIdAndTenantId(studentId, tenantId)
				.forEach(conversation -> eraseConversation(tenantId, conversation));
	}

	public void eraseConversation(Long tenantId, Conversation conversation) {
		Instant now = Instant.now();
		List<ConversationMessage> messages = messageRepository.findAllByConversationIdAndTenantId(conversation.getId(),
				tenantId);
		messages.forEach(message -> message.erase(now));
		messageRepository.saveAll(messages);
		List<MessageReport> reports = reportRepository.findAllByConversationIdAndTenantId(conversation.getId(),
				tenantId);
		reports.forEach(MessageReport::eraseReason);
		reportRepository.saveAll(reports);
	}

	public void deleteConversation(Long tenantId, Conversation conversation) {
		reportRepository.deleteAll(reportRepository.findAllByConversationIdAndTenantId(conversation.getId(), tenantId));
		messageRepository
				.deleteAll(messageRepository.findAllByConversationIdAndTenantId(conversation.getId(), tenantId));
		conversationRepository.delete(conversation);
	}

	public void deleteAnnouncement(Long tenantId, Announcement announcement) {
		notificationService.purgeBySource(announcement.sourceKey());
		recipientRepository.deleteByAnnouncement(tenantId, announcement.getId());
		announcementRepository.delete(announcement);
	}

	public Map<String, Object> exportForUser(Long tenantId, Long userId) {
		Map<String, Object> export = new HashMap<>();
		List<Map<String, Object>> messages = new ArrayList<>();
		for (ConversationMessage message : messageRepository.findAllBySenderUserIdAndTenantId(userId, tenantId)) {
			Map<String, Object> row = new HashMap<>();
			row.put("sentAt", message.getCreatedAt());
			row.put("body", message.getBody());
			messages.add(row);
		}
		export.put("messagesSent", messages);
		export.put("conversations", conversationRepository.findAllForParticipant(tenantId, userId).size());
		List<Map<String, Object>> received = new ArrayList<>();
		for (AnnouncementRecipient recipient : recipientRepository.findAllByUserIdAndTenantId(userId, tenantId)) {
			Map<String, Object> row = new HashMap<>();
			row.put("notifiedAt", recipient.getNotifiedAt());
			row.put("readAt", recipient.getReadAt());
			received.add(row);
		}
		export.put("announcementsReceived", received);
		return export;
	}
}

package com.altafjava.school.application.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.notification.model.NotificationChannel;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.announcement.AttachmentValidator;
import com.altafjava.school.domain.error.CommunicationErrorCode;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.model.ConversationMessage;
import com.altafjava.school.domain.messaging.model.MessageReport;
import com.altafjava.school.domain.messaging.repository.ConversationMessageRepository;
import com.altafjava.school.domain.messaging.repository.ConversationRepository;
import com.altafjava.school.domain.messaging.repository.MessageReportRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * One-to-one messaging between a guardian and a teacher of their child. Students cannot take part.
 * The notification names no content, so a message stays out of push previews and email.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

	private static final Set<NotificationChannel> ALERT_CHANNELS = Set.of(NotificationChannel.IN_APP,
			NotificationChannel.PUSH);

	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository messageRepository;
	private final MessageReportRepository reportRepository;
	private final GuardianRepository guardianRepository;
	private final TeacherRepository teacherRepository;
	private final StudentRepository studentRepository;
	private final MessagingRelationship relationship;
	private final MessagingSettingsService settingsService;
	private final AttachmentValidator attachmentValidator;
	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;

	public record NewConversation(String studentPublicId, String teacherPublicId, String guardianPublicId,
			String body, List<String> attachmentFilePublicIds) {
	}

	/** A guardian names the teacher; a teacher names the guardian. Reuses the thread if one exists. */
	@Transactional
	public Conversation start(NewConversation request, Long callerUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		settingsService.assertOpenNow();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(request.studentPublicId()),
				tenantId).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
		var callerGuardian = guardianRepository.findByUserIdAndTenantId(callerUserId, tenantId);
		Guardian guardian = callerGuardian.orElseGet(() -> requireGuardian(tenantId, request.guardianPublicId()));
		Teacher teacher = callerGuardian.isPresent()
				? requireTeacher(tenantId, request.teacherPublicId())
				: teacherRepository.findByUserIdAndTenantId(callerUserId, tenantId)
						.orElseThrow(() -> new AccessDeniedException("Only guardians and teachers can start one"));
		if (guardian.getUserId() == null || teacher.getUserId() == null) {
			throw new BusinessException(CommunicationErrorCode.RECIPIENT_HAS_NO_ACCOUNT,
					"The other person has no account yet");
		}
		relationship.assertAllowed(tenantId, guardian.getId(), teacher.getUserId(), student.getId());
		Conversation conversation = conversationRepository
				.findByGuardianIdAndTeacherIdAndStudentIdAndTenantId(guardian.getId(), teacher.getId(),
						student.getId(), tenantId)
				.orElseGet(() -> open(guardian, teacher, student));
		append(tenantId, conversation, callerUserId, request.body(), request.attachmentFilePublicIds());
		return conversation;
	}

	private Conversation open(Guardian guardian, Teacher teacher, Student student) {
		try {
			return conversationRepository.saveAndFlush(Conversation.open(guardian.getId(), guardian.getUserId(),
					teacher.getId(), teacher.getUserId(), student.getId()));
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(CommunicationErrorCode.CONVERSATION_JUST_STARTED,
					"This conversation was just started; open it and reply");
		}
	}

	@Transactional
	public ConversationMessage send(String conversationPublicId, String body, List<String> attachments,
			Long senderUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		settingsService.assertOpenNow();
		Conversation conversation = requireParticipant(tenantId, conversationPublicId, senderUserId);
		relationship.assertAllowed(tenantId, conversation.getGuardianId(), conversation.getTeacherUserId(),
				conversation.getStudentId());
		return append(tenantId, conversation, senderUserId, body, attachments);
	}

	private ConversationMessage append(Long tenantId, Conversation conversation, Long senderUserId, String body,
			List<String> attachments) {
		List<UUID> files = attachmentValidator.requireExisting(attachments == null ? List.of() : attachments);
		ConversationMessage message = messageRepository.save(
				ConversationMessage.send(conversation.getId(), senderUserId, body, files));
		conversationRepository.touch(tenantId, conversation.getId(), Instant.now());
		notificationService.send(SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(conversation.otherParticipant(senderUserId))
				.type(SchoolNotificationTypes.MESSAGE_RECEIVED)
				.title("New message")
				.message("You have a new message.")
				.priority(NotificationPriority.NORMAL)
				.requestedChannels(ALERT_CHANNELS)
				.metadata(objectMapper
						.writeValueAsString(Map.of("conversationPublicId", conversation.getPublicId().toString())))
				.build());
		return message;
	}

	@Transactional(readOnly = true)
	public Page<ConversationSummary> list(Long userId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Page<Conversation> page = conversationRepository.findForParticipant(tenantId, userId, pageable);
		if (page.isEmpty()) {
			return page.map(conversation -> new ConversationSummary(conversation, 0));
		}
		Map<Long, Long> unread = messageRepository
				.countUnreadByConversation(tenantId, userId, page.map(Conversation::getId).getContent()).stream()
				.collect(Collectors.toMap(ConversationMessageRepository.UnreadCount::conversationId,
						ConversationMessageRepository.UnreadCount::unread));
		return page.map(conversation -> new ConversationSummary(conversation,
				unread.getOrDefault(conversation.getId(), 0L)));
	}

	@Transactional(readOnly = true)
	public Page<ConversationMessage> messages(String conversationPublicId, Long userId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Conversation conversation = requireParticipant(tenantId, conversationPublicId, userId);
		Page<ConversationMessage> page = messageRepository
				.findByConversationIdAndTenantIdOrderByIdDesc(conversation.getId(), tenantId, pageable);
		page.forEach(ConversationMessage::loadDetails);
		return page;
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return messageRepository.countUnread(TenantContext.getCurrentTenantId(), userId);
	}

	@Transactional
	public void markRead(String conversationPublicId, Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Conversation conversation = requireParticipant(tenantId, conversationPublicId, userId);
		messageRepository.findFirstByConversationIdAndTenantIdOrderByIdDesc(conversation.getId(), tenantId)
				.ifPresent(last -> {
					conversation.markReadUpTo(userId, last.getId());
					conversationRepository.save(conversation);
				});
	}

	/** A participant flags the other person's message for the school's reviewers. */
	@Transactional
	public MessageReport report(String conversationPublicId, String messagePublicId, String reason, Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Conversation conversation = requireParticipant(tenantId, conversationPublicId, userId);
		ConversationMessage message = messageRepository
				.findByPublicIdAndTenantId(UUID.fromString(messagePublicId), tenantId)
				.filter(found -> found.getConversationId().equals(conversation.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messagePublicId));
		if (message.getSenderUserId().equals(userId)) {
			throw new BusinessException(CommunicationErrorCode.REPORT_OWN_MESSAGE,
					"You cannot report your own message");
		}
		if (reportRepository.existsByMessageIdAndReportedByUserIdAndTenantId(message.getId(), userId, tenantId)) {
			throw new BusinessException(CommunicationErrorCode.MESSAGE_ALREADY_REPORTED,
					"You have already reported this message");
		}
		return reportRepository.save(MessageReport.file(message, userId, reason));
	}

	private Conversation requireParticipant(Long tenantId, String publicId, Long userId) {
		Conversation conversation = conversationRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + publicId));
		if (!conversation.hasParticipant(userId)) {
			throw new AccessDeniedException("Only its two participants can use this conversation");
		}
		return conversation;
	}

	private Guardian requireGuardian(Long tenantId, String publicId) {
		if (publicId == null) {
			throw new BusinessException(CommunicationErrorCode.CONVERSATION_GUARDIAN_REQUIRED,
					"Name the guardian to message");
		}
		return guardianRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + publicId));
	}

	private Teacher requireTeacher(Long tenantId, String publicId) {
		if (publicId == null) {
			throw new BusinessException(CommunicationErrorCode.CONVERSATION_TEACHER_REQUIRED,
					"Name the teacher to message");
		}
		return teacherRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + publicId));
	}
}

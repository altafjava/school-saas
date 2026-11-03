package com.altafjava.school.application.messaging;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.model.ConversationMessage;
import com.altafjava.school.domain.messaging.model.MessageReport;
import com.altafjava.school.domain.messaging.model.MessageReportStatus;
import com.altafjava.school.domain.messaging.repository.ConversationMessageRepository;
import com.altafjava.school.domain.messaging.repository.ConversationRepository;
import com.altafjava.school.domain.messaging.repository.MessageReportRepository;
import lombok.RequiredArgsConstructor;

/**
 * What a named reviewer can do: read, and close reports, never post. Opening a conversation writes
 * an audit entry naming the reviewer, in the same transaction as the read, and no content.
 */
@Service
@RequiredArgsConstructor
public class ConversationReviewService {

	private static final String RESOURCE_TYPE = "Conversation";

	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository messageRepository;
	private final MessageReportRepository reportRepository;
	private final ActivityLogService activityLogService;

	@Transactional(readOnly = true)
	public Page<Conversation> list(Pageable pageable) {
		return conversationRepository.findAllForReview(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional
	public Page<ConversationMessage> messages(String conversationPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Conversation conversation = conversationRepository
				.findByPublicIdAndTenantId(UUID.fromString(conversationPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationPublicId));
		audit(tenantId, conversationPublicId, AuditAction.SENSITIVE_ACCESS, "Conversation opened for review");
		Page<ConversationMessage> page = messageRepository
				.findByConversationIdAndTenantIdOrderByIdDesc(conversation.getId(), tenantId, pageable);
		page.forEach(ConversationMessage::loadDetails);
		return page;
	}

	@Transactional(readOnly = true)
	public Page<MessageReport> reports(MessageReportStatus status, Pageable pageable) {
		return reportRepository.search(TenantContext.getCurrentTenantId(), status, pageable);
	}

	@Transactional
	public MessageReport resolve(String reportPublicId, String note, Long reviewerUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		MessageReport report = reportRepository.findByPublicIdAndTenantId(UUID.fromString(reportPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportPublicId));
		report.resolve(reviewerUserId, note, Instant.now());
		audit(tenantId, reportPublicId, AuditAction.UPDATE, "Message report resolved");
		return reportRepository.save(report);
	}

	private void audit(Long tenantId, String resourceId, AuditAction action, String details) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String actorId = authentication != null ? authentication.getName() : "system";
		activityLogService.log(tenantId, action.name(), RESOURCE_TYPE, resourceId, actorId, null, null, details, null,
				null);
	}
}

package com.altafjava.school.application.announcement;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.security.PermissionAuthorizationService;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementChannel;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;
import com.altafjava.school.domain.announcement.model.AudienceType;
import com.altafjava.school.domain.announcement.repository.AnnouncementRecipientRepository;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/**
 * Announcement authoring and the reader's feed. Holders of {@code ANNOUNCEMENT_MANAGE} reach any
 * audience; a teacher with {@code ANNOUNCEMENT_CLASS_POST} reaches only classrooms they teach.
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

	private final AnnouncementRepository announcementRepository;
	private final AnnouncementRecipientRepository recipientRepository;
	private final AnnouncementPublisher publisher;
	private final AudienceResolver audienceResolver;
	private final AttachmentValidator attachmentValidator;
	private final AcademicScopeResolver academicScopeResolver;
	private final PermissionAuthorizationService permissionAuthorizationService;
	private final NotificationService notificationService;

	public record FeedItem(Announcement announcement, Instant readAt) {
	}

	public record Draft(String title, String body, AudienceType audienceType, Set<String> targets,
			boolean includeStudents, Set<AnnouncementChannel> channels,
			List<String> attachmentFilePublicIds, Instant expiresAt) {
	}

	@Transactional
	public Announcement create(Draft draft, Long authorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = Announcement.draft(authorUserId, content(draft));
		assertMayTarget(tenantId, announcement);
		return announcementRepository.save(announcement);
	}

	@Transactional
	public Announcement update(String publicId, Draft draft, ExpectedVersion expectedVersion, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = requireEditable(tenantId, publicId, actorUserId);
		expectedVersion.verify(announcement);
		announcement.revise(content(draft));
		assertMayTarget(tenantId, announcement);
		return announcementRepository.save(announcement);
	}

	@Transactional
	public Announcement schedule(String publicId, Instant publishAt, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = requireEditable(tenantId, publicId, actorUserId);
		announcement.schedule(publishAt, Instant.now());
		return announcementRepository.save(announcement);
	}

	@Transactional
	public Announcement publish(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = requireEditable(tenantId, publicId, actorUserId);
		return publishToSomeone(tenantId, announcement);
	}

	@Transactional
	public Announcement cancel(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = requireEditable(tenantId, publicId, actorUserId);
		announcement.cancel();
		return announcementRepository.save(announcement);
	}

	@Transactional
	public void delete(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = requireEditable(tenantId, publicId, actorUserId);
		if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
			throw new BusinessException("Only a draft can be deleted; cancel a scheduled announcement instead");
		}
		announcement.softDelete(String.valueOf(actorUserId));
		announcementRepository.save(announcement);
	}

	/** Sent at once on every channel; preferences and quiet hours do not apply. */
	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "EmergencyAlert", details = "Emergency alert sent")
	public Announcement sendEmergency(Draft draft, Long authorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = announcementRepository.save(Announcement.emergency(authorUserId, content(draft)));
		return publishToSomeone(tenantId, announcement);
	}

	// Throwing rolls the publish back, so a person is told instead of sending to nobody.
	private Announcement publishToSomeone(Long tenantId, Announcement announcement) {
		Announcement published = publisher.publish(tenantId, announcement.getId());
		if (published.getRecipientCount() == 0) {
			throw new BusinessException("Nobody in this audience has an account to receive it");
		}
		return published;
	}

	@Transactional(readOnly = true)
	public Page<Announcement> list(AnnouncementStatus status, Long actorUserId, Pageable pageable) {
		Page<Announcement> page = announcementRepository.search(TenantContext.getCurrentTenantId(), managesAll(),
				actorUserId, status, pageable);
		page.forEach(Announcement::loadDetails);
		return page;
	}

	@Transactional(readOnly = true)
	public Announcement get(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = require(tenantId, publicId);
		assertAuthorOrManager(announcement, actorUserId);
		return announcement;
	}

	@Transactional(readOnly = true)
	public AnnouncementDeliveryReport deliveryReport(String publicId, Long actorUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = require(tenantId, publicId);
		assertAuthorOrManager(announcement, actorUserId);
		return new AnnouncementDeliveryReport(announcement.getRecipientCount(),
				recipientRepository.countByAnnouncementIdAndTenantIdAndReadAtIsNotNull(announcement.getId(), tenantId),
				announcement.getDispatchedAt() != null,
				notificationService.deliverySummary(announcement.sourceKey()));
	}

	@Transactional(readOnly = true)
	public Page<AnnouncementRecipient> recipients(String publicId, Long actorUserId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = require(tenantId, publicId);
		assertAuthorOrManager(announcement, actorUserId);
		return recipientRepository.findByAnnouncementIdAndTenantIdOrderByIdAsc(announcement.getId(), tenantId,
				pageable);
	}

	@Transactional(readOnly = true)
	public Page<FeedItem> feed(Long userId, boolean unreadOnly, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Page<Announcement> page = announcementRepository.findFeed(tenantId, userId, Instant.now(), unreadOnly,
				pageable);
		page.forEach(Announcement::loadDetails);
		Map<Long, Instant> readAt = new HashMap<>();
		if (page.hasContent()) {
			recipientRepository.findAllByUserIdAndTenantIdAndAnnouncementIdIn(userId, tenantId,
					page.map(Announcement::getId).getContent())
					.forEach(recipient -> readAt.put(recipient.getAnnouncementId(), recipient.getReadAt()));
		}
		return page.map(announcement -> new FeedItem(announcement, readAt.get(announcement.getId())));
	}

	@Transactional(readOnly = true)
	public long unreadCount(Long userId) {
		return recipientRepository.countUnread(TenantContext.getCurrentTenantId(), userId, Instant.now());
	}

	@Transactional(readOnly = true)
	public FeedItem readFromFeed(String publicId, Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = require(tenantId, publicId);
		return new FeedItem(announcement, requireRecipient(tenantId, announcement, userId).getReadAt());
	}

	@Transactional
	public void markRead(String publicId, Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Announcement announcement = require(tenantId, publicId);
		AnnouncementRecipient recipient = requireRecipient(tenantId, announcement, userId);
		recipient.markRead(Instant.now());
		recipientRepository.save(recipient);
		notificationService.markSourceRead(userId, announcement.sourceKey());
	}

	// Not addressed to the caller reads the same as not existing.
	private AnnouncementRecipient requireRecipient(Long tenantId, Announcement announcement, Long userId) {
		return recipientRepository.findByAnnouncementIdAndUserIdAndTenantId(announcement.getId(), userId, tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Announcement not found: " + announcement.getPublicId()));
	}

	private Announcement.Content content(Draft draft) {
		Set<String> targets = draft.targets() == null ? Set.of() : draft.targets();
		List<String> attachments = draft.attachmentFilePublicIds() == null ? List.of()
				: draft.attachmentFilePublicIds();
		Set<AnnouncementChannel> channels = draft.channels() == null
				? Announcement.ALL_CHANNELS
				: draft.channels();
		List<UUID> files = attachmentValidator.requireExisting(attachments);
		return new Announcement.Content(draft.title(), draft.body(), draft.audienceType(), targets,
				draft.includeStudents(), channels, files, draft.expiresAt());
	}

	private Announcement require(Long tenantId, String publicId) {
		Announcement announcement = announcementRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Announcement not found: " + publicId));
		announcement.loadDetails();
		return announcement;
	}

	private Announcement requireEditable(Long tenantId, String publicId, Long actorUserId) {
		Announcement announcement = require(tenantId, publicId);
		assertAuthorOrManager(announcement, actorUserId);
		assertMayTarget(tenantId, announcement);
		return announcement;
	}

	private void assertAuthorOrManager(Announcement announcement, Long actorUserId) {
		if (!managesAll() && !announcement.getCreatedByUserId().equals(actorUserId)) {
			throw new AccessDeniedException("Only its author or an announcement manager can use this announcement");
		}
	}

	private void assertMayTarget(Long tenantId, Announcement announcement) {
		if (managesAll()) {
			return;
		}
		if (announcement.getAudienceType() != AudienceType.CLASSROOM) {
			throw new AccessDeniedException("Teachers can only post to classrooms");
		}
		AcademicScope scope = academicScopeResolver.current(tenantId);
		boolean teachesAll = audienceResolver.targetClassrooms(tenantId, announcement.getTargets()).stream()
				.allMatch(scope.teaching()::teachesClassroom);
		if (!teachesAll) {
			throw new AccessDeniedException("You can only post to classrooms you teach");
		}
	}

	private boolean managesAll() {
		return permissionAuthorizationService.hasPermission(SchoolPermissions.ANNOUNCEMENT_MANAGE);
	}
}

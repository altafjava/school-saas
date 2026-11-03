package com.altafjava.school.application.announcement;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.domain.notification.model.NotificationChannel;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationTypes;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;
import com.altafjava.school.domain.announcement.repository.AnnouncementRecipientRepository;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import lombok.RequiredArgsConstructor;

/** One committed slice of an announcement's sending, so a crash resumes instead of resending. */
@Component
@RequiredArgsConstructor
class AnnouncementDispatchBatch {

	private final AnnouncementRepository announcementRepository;
	private final AnnouncementRecipientRepository recipientRepository;
	private final NotificationService notificationService;
	private final UserRepository userRepository;

	/** @return whether another batch may remain */
	@Transactional
	public boolean sendNext(Long tenantId, Long announcementId) {
		Announcement announcement = announcementRepository.findByIdAndTenantIdForUpdate(announcementId, tenantId)
				.orElse(null);
		if (announcement == null || announcement.getDispatchedAt() != null) {
			return false;
		}
		List<AnnouncementRecipient> batch = recipientRepository
				.findTop200ByAnnouncementIdAndTenantIdAndNotifiedAtIsNullOrderByIdAsc(announcementId, tenantId);
		if (batch.isEmpty()) {
			announcement.markDispatched(Instant.now());
			announcementRepository.save(announcement);
			return false;
		}
		batch.forEach(recipient -> notify(tenantId, announcement, recipient));
		recipientRepository.saveAll(batch);
		return true;
	}

	// An account removed since publish is passed over: failing here would poison the whole batch.
	private void notify(Long tenantId, Announcement announcement, AnnouncementRecipient recipient) {
		if (userRepository.findById(recipient.getUserId()).isPresent()) {
			notificationService.send(command(tenantId, announcement, recipient.getUserId()));
		}
		recipient.markNotified(Instant.now());
	}

	private SendNotificationCommand command(Long tenantId, Announcement announcement, Long userId) {
		Set<NotificationChannel> channels = announcement.getChannels().stream()
				.map(channel -> NotificationChannel.valueOf(channel.name()))
				.collect(Collectors.toSet());
		SendNotificationCommand.Builder builder = SendNotificationCommand.builder()
				.tenantId(tenantId)
				.userId(userId)
				.title(announcement.getTitle())
				.message(announcement.getBody())
				.templateVariables(Map.of("title", announcement.getTitle(), "body", announcement.getBody()))
				.sourceKey(announcement.sourceKey());
		if (announcement.isEmergency()) {
			return builder.type(SchoolNotificationTypes.EMERGENCY_ALERT)
					.priority(NotificationPriority.URGENT)
					.forceChannels(channels)
					.build();
		}
		return builder.type(NotificationTypes.ANNOUNCEMENT)
				.priority(NotificationPriority.NORMAL)
				.requestedChannels(channels)
				.build();
	}
}

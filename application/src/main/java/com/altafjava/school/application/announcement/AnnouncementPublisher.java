package com.altafjava.school.application.announcement;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.school.domain.announcement.event.AnnouncementPublishedEvent;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementRecipient;
import com.altafjava.school.domain.announcement.repository.AnnouncementRecipientRepository;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;

/** Freezes the audience and publishes, in one transaction, for both a person and the scheduler. */
@Component
@RequiredArgsConstructor
public class AnnouncementPublisher {

	private final AnnouncementRepository announcementRepository;
	private final AnnouncementRecipientRepository recipientRepository;
	private final AudienceResolver audienceResolver;
	private final EventPublisher eventPublisher;

	@Transactional
	public Announcement publish(Long tenantId, Long announcementId) {
		Announcement announcement = announcementRepository.findByIdAndTenantIdForUpdate(announcementId, tenantId)
				.orElseThrow(() -> new IllegalStateException("Announcement not found: " + announcementId));
		Set<Long> users = audienceResolver.resolve(tenantId, announcement);
		announcement.publish(users.size(), Instant.now());
		Announcement saved = announcementRepository.save(announcement);
		List<AnnouncementRecipient> recipients = users.stream()
				.map(userId -> AnnouncementRecipient.of(saved.getId(), userId))
				.toList();
		recipientRepository.saveAll(recipients);
		eventPublisher.publish(new AnnouncementPublishedEvent(tenantId, saved.getId()));
		return saved;
	}
}

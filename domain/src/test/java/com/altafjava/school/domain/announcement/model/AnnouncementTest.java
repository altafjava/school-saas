package com.altafjava.school.domain.announcement.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class AnnouncementTest {

	private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

	private static Announcement.Content content(AudienceType type, Set<String> targets, Instant expiresAt) {
		return new Announcement.Content("Title", "Body", type, targets, false, Set.of(AnnouncementChannel.IN_APP),
				List.of(), expiresAt);
	}

	private static Announcement draft() {
		return Announcement.draft(1L, content(AudienceType.SCHOOL, Set.of(), null));
	}

	@Test
	void draft_needsTargetsForAnyAudienceButTheWholeSchool() {
		assertThrows(BusinessException.class,
				() -> Announcement.draft(1L, content(AudienceType.CLASSROOM, Set.of(), null)));
	}

	@Test
	void draft_needsAChannel() {
		assertThrows(BusinessException.class, () -> Announcement.draft(1L, new Announcement.Content("T", "B",
				AudienceType.SCHOOL, Set.of(), false, Set.of(), List.of(), null)));
	}

	@Test
	void emergency_alwaysUsesEveryChannel() {
		Announcement alert = Announcement.emergency(1L, content(AudienceType.SCHOOL, Set.of(), null));

		assertTrue(alert.isEmergency());
		assertEquals(Set.of(AnnouncementChannel.values()), alert.getChannels());
	}

	@Test
	void schedule_refusesAPastTime() {
		assertThrows(BusinessException.class, () -> draft().schedule(NOW.minusSeconds(1), NOW));
	}

	@Test
	void publish_refusesAnAnnouncementThatAlreadyExpired() {
		Announcement announcement = Announcement.draft(1L, content(AudienceType.SCHOOL, Set.of(), NOW));

		assertThrows(BusinessException.class, () -> announcement.publish(3, NOW));
	}

	@Test
	void published_cannotBeChanged_orPublishedTwice() {
		Announcement announcement = draft();
		announcement.publish(3, NOW);

		assertEquals(AnnouncementStatus.PUBLISHED, announcement.getStatus());
		assertEquals(3, announcement.getRecipientCount());
		assertThrows(BusinessException.class, () -> announcement.publish(3, NOW));
		assertThrows(BusinessException.class, announcement::cancel);
		assertThrows(BusinessException.class,
				() -> announcement.revise(content(AudienceType.SCHOOL, Set.of(), null)));
	}

	@Test
	void expire_onlyAppliesToAPublishedAnnouncement() {
		Announcement announcement = draft();

		announcement.expire();
		assertEquals(AnnouncementStatus.DRAFT, announcement.getStatus());

		announcement.publish(1, NOW);
		announcement.expire();
		assertEquals(AnnouncementStatus.EXPIRED, announcement.getStatus());
	}
}

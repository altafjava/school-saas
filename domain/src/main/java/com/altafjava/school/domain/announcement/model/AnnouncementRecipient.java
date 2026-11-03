package com.altafjava.school.domain.announcement.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.altafjava.platform.core.model.TenantEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One person an announcement reached, frozen at publish time, so later class moves change nothing. */
@Entity
@Table(name = "announcement_recipients")
@Getter
@SuperBuilder
@NoArgsConstructor
public class AnnouncementRecipient extends TenantEntity {

	@Column(name = "announcement_id", nullable = false, updatable = false)
	private Long announcementId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "notified_at")
	private Instant notifiedAt;

	@Column(name = "read_at")
	private Instant readAt;

	public static AnnouncementRecipient of(Long announcementId, Long userId) {
		return AnnouncementRecipient.builder().announcementId(announcementId).userId(userId).build();
	}

	public void markNotified(Instant now) {
		this.notifiedAt = now;
	}

	public void markRead(Instant now) {
		if (readAt == null) {
			this.readAt = now;
		}
	}
}

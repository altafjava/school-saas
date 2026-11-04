package com.altafjava.school.domain.announcement.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.school.domain.error.CommunicationErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** A notice or circular: drafted, optionally scheduled, then published once to a frozen audience. */
@Entity
@Table(name = "announcements")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Announcement extends SoftDeletableEntity {

	/** Every channel an emergency alert goes out on. */
	public static final Set<AnnouncementChannel> ALL_CHANNELS = Set.of(AnnouncementChannel.values());

	private static final String ERASED = "[erased]";

	@Column(name = "created_by_user_id", nullable = false, updatable = false)
	private Long createdByUserId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "body", nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "include_students", nullable = false)
	private boolean includeStudents;

	@Column(name = "emergency", nullable = false, updatable = false)
	private boolean emergency;

	@Column(name = "recipient_count", nullable = false)
	private int recipientCount;

	@Column(name = "publish_at")
	private Instant publishAt;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	// Set once every recipient has been handed to the notification service.
	@Column(name = "dispatched_at")
	private Instant dispatchedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "audience_type", nullable = false, length = 20)
	private AudienceType audienceType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private AnnouncementStatus status = AnnouncementStatus.DRAFT;

	// Grade names, role names, or the public ids of classrooms or guardians, per audienceType.
	@ElementCollection(fetch = FetchType.LAZY)
	@BatchSize(size = 50)
	@CollectionTable(name = "announcement_targets", joinColumns = @JoinColumn(name = "announcement_id"))
	@Column(name = "target_value", nullable = false, length = 100)
	@Builder.Default
	private Set<String> targets = new HashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@BatchSize(size = 50)
	@CollectionTable(name = "announcement_channels", joinColumns = @JoinColumn(name = "announcement_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 20)
	@Builder.Default
	private Set<AnnouncementChannel> channels = new HashSet<>();

	// Uploaded through /api/v1/files first, as with StudentLeaveRequest.attachmentFilePublicId.
	@ElementCollection(fetch = FetchType.LAZY)
	@BatchSize(size = 50)
	@CollectionTable(name = "announcement_attachments", joinColumns = @JoinColumn(name = "announcement_id"))
	@Column(name = "file_public_id", nullable = false)
	@Builder.Default
	private List<UUID> attachmentFilePublicIds = new ArrayList<>();

	public record Content(String title, String body, AudienceType audienceType, Set<String> targets,
			boolean includeStudents, Set<AnnouncementChannel> channels, List<UUID> attachments, Instant expiresAt) {

		public Content {
			targets = Set.copyOf(targets);
			channels = Set.copyOf(channels);
			attachments = List.copyOf(attachments);
		}
	}

	public static Announcement draft(Long createdByUserId, Content content) {
		Announcement announcement = Announcement.builder().createdByUserId(createdByUserId).build();
		announcement.apply(content);
		return announcement;
	}

	public static Announcement emergency(Long createdByUserId, Content content) {
		Announcement announcement = Announcement.builder().createdByUserId(createdByUserId).emergency(true).build();
		announcement.apply(new Content(content.title(), content.body(), content.audienceType(), content.targets(),
				content.includeStudents(), ALL_CHANNELS, content.attachments(), content.expiresAt()));
		return announcement;
	}

	public void revise(Content content) {
		assertEditable();
		apply(content);
	}

	private void apply(Content content) {
		if (content.audienceType() != AudienceType.SCHOOL && content.targets().isEmpty()) {
			throw new BusinessException(CommunicationErrorCode.AUDIENCE_TARGETS_REQUIRED,
					"Choose at least one target for this audience");
		}
		if (content.channels().isEmpty()) {
			throw new BusinessException(CommunicationErrorCode.CHANNELS_REQUIRED,
					"Choose at least one delivery channel");
		}
		this.title = content.title();
		this.body = content.body();
		this.audienceType = content.audienceType();
		this.includeStudents = content.includeStudents();
		this.expiresAt = content.expiresAt();
		this.targets = new HashSet<>(content.targets());
		this.channels = new HashSet<>(content.channels());
		this.attachmentFilePublicIds = new ArrayList<>(content.attachments());
	}

	/** Groups the notifications this announcement fanned out to. */
	public String sourceKey() {
		return "announcement:" + getPublicId();
	}

	/** Open-in-view is off, so a service loads these inside its transaction for the response. */
	public void loadDetails() {
		Hibernate.initialize(targets);
		Hibernate.initialize(channels);
		Hibernate.initialize(attachmentFilePublicIds);
	}

	public void schedule(Instant publishAt, Instant now) {
		assertEditable();
		if (!publishAt.isAfter(now)) {
			throw new BusinessException(CommunicationErrorCode.PUBLISH_TIME_NOT_IN_FUTURE,
					"The publish time must be in the future");
		}
		assertNotExpiredAt(publishAt);
		this.publishAt = publishAt;
		this.status = AnnouncementStatus.SCHEDULED;
	}

	public void publish(int recipientCount, Instant now) {
		assertEditable();
		assertNotExpiredAt(now);
		this.recipientCount = recipientCount;
		this.publishedAt = now;
		this.status = AnnouncementStatus.PUBLISHED;
	}

	public void markDispatched(Instant now) {
		this.dispatchedAt = now;
	}

	public void cancel() {
		assertEditable();
		this.status = AnnouncementStatus.CANCELLED;
	}

	public void expire() {
		if (status == AnnouncementStatus.PUBLISHED) {
			this.status = AnnouncementStatus.EXPIRED;
		}
	}

	public void erasePii() {
		this.title = ERASED;
		this.body = ERASED;
		this.targets = new HashSet<>();
		this.attachmentFilePublicIds = new ArrayList<>();
	}

	private void assertEditable() {
		if (status != AnnouncementStatus.DRAFT && status != AnnouncementStatus.SCHEDULED) {
			throw new BusinessException(CommunicationErrorCode.ANNOUNCEMENT_NOT_EDITABLE,
					"Only a draft or scheduled announcement can be changed");
		}
	}

	private void assertNotExpiredAt(Instant moment) {
		if (expiresAt != null && !expiresAt.isAfter(moment)) {
			throw new BusinessException(CommunicationErrorCode.ANNOUNCEMENT_ALREADY_EXPIRED,
					"The announcement would already have expired");
		}
	}
}

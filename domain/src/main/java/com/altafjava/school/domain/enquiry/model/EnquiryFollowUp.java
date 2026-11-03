package com.altafjava.school.domain.enquiry.model;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** One contact with the family about an enquiry, and what it led to. */
@Entity
@Table(name = "enquiry_follow_ups")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class EnquiryFollowUp extends SoftDeletableEntity {

	// FK to enquiries.id
	@Column(name = "enquiry_id", nullable = false)
	private Long enquiryId;

	// FK to platform users.id
	@Column(name = "recorded_by_user_id")
	private Long recordedByUserId;

	@Column(name = "followed_up_at", nullable = false)
	private Instant followedUpAt;

	@Column(name = "note", nullable = false, length = 2000)
	private String note;

	@Column(name = "next_follow_up_on")
	private LocalDate nextFollowUpOn;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 20)
	private FollowUpChannel channel;

	public static EnquiryFollowUp record(Long enquiryId, FollowUpChannel channel, String note,
			LocalDate nextFollowUpOn, Long recordedByUserId) {
		return EnquiryFollowUp.builder()
				.enquiryId(enquiryId)
				.channel(channel)
				.note(note)
				.nextFollowUpOn(nextFollowUpOn)
				.recordedByUserId(recordedByUserId)
				.followedUpAt(Instant.now())
				.build();
	}

	public void erasePii() {
		this.note = "erased";
	}
}

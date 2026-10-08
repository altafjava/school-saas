package com.altafjava.school.domain.counseling.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * {@code reason} is {@code @Pii}-flagged, matching {@code CounselingSession.notes} — the same
 * confidentiality posture as Health's PHI-grade fields. {@code counselingSessionId} is set once the
 * referral is scheduled against an actual {@link CounselingSession}.
 */
@Entity
@Table(name = "counseling_referrals")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CounselingReferral extends SoftDeletableEntity {

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "counseling_session_id")
	private Long counselingSessionId;

	@Column(name = "referred_by_user_id", nullable = false)
	private Long referredByUserId;

	@Pii
	@Column(name = "reason", nullable = false, length = 1000)
	private String reason;

	@Column(name = "referred_at", nullable = false)
	private LocalDateTime referredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CounselingReferralStatus status;

	public static CounselingReferral refer(Long studentId, Long referredByUserId, String reason) {
		return CounselingReferral.builder()
				.studentId(studentId)
				.referredByUserId(referredByUserId)
				.reason(reason)
				.referredAt(LocalDateTime.now())
				.status(CounselingReferralStatus.PENDING)
				.build();
	}

	public void scheduleWithSession(Long counselingSessionId) {
		requireStatus(CounselingReferralStatus.PENDING, "schedule");
		this.status = CounselingReferralStatus.SCHEDULED;
		this.counselingSessionId = counselingSessionId;
	}

	public void complete() {
		requireStatus(CounselingReferralStatus.SCHEDULED, "complete");
		this.status = CounselingReferralStatus.COMPLETED;
	}

	public void decline() {
		requireStatus(CounselingReferralStatus.PENDING, "decline");
		this.status = CounselingReferralStatus.DECLINED;
	}

	private void requireStatus(CounselingReferralStatus required, String action) {
		if (this.status != required) {
			throw new BusinessException("Cannot " + action + " a counseling referral in status " + this.status);
		}
	}
}

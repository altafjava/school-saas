package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;
import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;

public record CounselingReferralResponse(
		String publicId,
		Long version,
		String studentPublicId,
		ReferenceSummary student,
		String referredByUserPublicId,
		ReferenceSummary referredByUser,
		String reason,
		LocalDateTime referredAt,
		CounselingReferralStatus status,
		@NoLabel("Has no human name") String counselingSessionPublicId) {
}

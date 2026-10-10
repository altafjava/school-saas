package com.altafjava.school.api.dto.response;

import java.time.LocalDateTime;
import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;

public record CounselingReferralResponse(
		String publicId,
		Long version,
		String studentPublicId,
		String referredByUserPublicId,
		String reason,
		LocalDateTime referredAt,
		CounselingReferralStatus status,
		String counselingSessionPublicId) {
}

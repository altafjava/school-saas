package com.altafjava.school.application.filter;

import com.altafjava.school.domain.counseling.model.CounselingReferralStatus;

/** Narrows the counselling-referral list; every part is optional. */
public record CounselingReferralFilter(String studentPublicId, CounselingReferralStatus status, DateWindow dates) {

	public static final CounselingReferralFilter NONE = new CounselingReferralFilter(null, null, DateWindow.UNBOUNDED);
}

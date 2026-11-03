package com.altafjava.school.domain.counseling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class CounselingReferralTest {

	private CounselingReferral referred() {
		return CounselingReferral.refer(1L, 99L, "Struggling academically and socially withdrawn", LocalDateTime.now());
	}

	@Test
	void refer_setsFieldsAndDefaultsPendingStatus() {
		CounselingReferral referral = referred();

		assertEquals(1L, referral.getStudentId());
		assertEquals(99L, referral.getReferredByUserId());
		assertEquals(CounselingReferralStatus.PENDING, referral.getStatus());
	}

	@Test
	void scheduleWithSession_fromPending_setsScheduledStatusAndSessionLink() {
		CounselingReferral referral = referred();

		referral.scheduleWithSession(42L);

		assertEquals(CounselingReferralStatus.SCHEDULED, referral.getStatus());
		assertEquals(42L, referral.getCounselingSessionId());
	}

	@Test
	void scheduleWithSession_whenNotPending_throwsBusinessException() {
		CounselingReferral referral = referred();
		referral.scheduleWithSession(42L);

		assertThrows(BusinessException.class, () -> referral.scheduleWithSession(43L));
	}

	@Test
	void complete_fromScheduled_setsCompletedStatus() {
		CounselingReferral referral = referred();
		referral.scheduleWithSession(42L);

		referral.complete();

		assertEquals(CounselingReferralStatus.COMPLETED, referral.getStatus());
	}

	@Test
	void complete_whenPending_throwsBusinessException() {
		CounselingReferral referral = referred();

		assertThrows(BusinessException.class, referral::complete);
	}

	@Test
	void decline_fromPending_setsDeclinedStatus() {
		CounselingReferral referral = referred();

		referral.decline();

		assertEquals(CounselingReferralStatus.DECLINED, referral.getStatus());
	}

	@Test
	void decline_whenAlreadyScheduled_throwsBusinessException() {
		CounselingReferral referral = referred();
		referral.scheduleWithSession(42L);

		assertThrows(BusinessException.class, referral::decline);
	}
}

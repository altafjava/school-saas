package com.altafjava.school.domain.visitor.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class VisitorRequestTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	private VisitorRequest walkIn() {
		return VisitorRequest.walkIn("Alex Ray", "555-0100", "Delivery", 7L, 3L, TODAY);
	}

	private VisitorRequest preRegisteredFor(LocalDate date) {
		return VisitorRequest.preRegister("Alex Ray", "555-0100", "Meeting", 7L, date, 3L, TODAY);
	}

	@Test
	void walkIn_isForTodayAndStartsPending() {
		VisitorRequest request = walkIn();

		assertEquals(TODAY, request.getVisitDate());
		assertEquals(VisitorRequestSource.WALK_IN, request.getSource());
		assertEquals(VisitorRequestStatus.PENDING, request.getStatus());
	}

	@Test
	void preRegister_forALaterDay_startsPending() {
		VisitorRequest request = preRegisteredFor(TODAY.plusDays(3));

		assertEquals(VisitorRequestSource.PRE_REGISTERED, request.getSource());
		assertEquals(VisitorRequestStatus.PENDING, request.getStatus());
	}

	@Test
	void preRegister_forADateThatHasPassed_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> preRegisteredFor(TODAY.minusDays(1)));
	}

	@Test
	void approve_pending_recordsWhoAndWhen() {
		VisitorRequest request = walkIn();

		request.approve(11L, TODAY);

		assertEquals(VisitorRequestStatus.APPROVED, request.getStatus());
		assertEquals(11L, request.getDecidedByUserId());
		assertNotNull(request.getDecidedAt());
	}

	@Test
	void approve_aVisitWhoseDateHasPassed_throwsBusinessException() {
		VisitorRequest request = preRegisteredFor(TODAY.plusDays(1));

		assertThrows(BusinessException.class, () -> request.approve(11L, TODAY.plusDays(2)));
	}

	@Test
	void approve_twice_throwsBusinessException() {
		VisitorRequest request = walkIn();
		request.approve(11L, TODAY);

		assertThrows(BusinessException.class, () -> request.approve(11L, TODAY));
	}

	@Test
	void reject_pending_keepsTheReason() {
		VisitorRequest request = walkIn();

		request.reject(11L, "Not expected");

		assertEquals(VisitorRequestStatus.REJECTED, request.getStatus());
		assertEquals("Not expected", request.getDecisionReason());
	}

	@Test
	void reject_anApprovedRequest_throwsBusinessException() {
		VisitorRequest request = walkIn();
		request.approve(11L, TODAY);

		assertThrows(BusinessException.class, () -> request.reject(11L, "late"));
	}

	@Test
	void cancel_pendingOrApproved_isAllowedButNotAfterwards() {
		VisitorRequest pending = walkIn();
		VisitorRequest approved = walkIn();
		approved.approve(11L, TODAY);

		pending.cancel();
		approved.cancel();

		assertEquals(VisitorRequestStatus.CANCELLED, pending.getStatus());
		assertEquals(VisitorRequestStatus.CANCELLED, approved.getStatus());
		assertThrows(BusinessException.class, pending::cancel);
	}

	@Test
	void requireAdmissibleOn_onlyForAnApprovedVisitOnItsOwnDate() {
		VisitorRequest request = preRegisteredFor(TODAY.plusDays(1));
		assertThrows(BusinessException.class, () -> request.requireAdmissibleOn(TODAY.plusDays(1)));

		request.approve(11L, TODAY);

		assertThrows(BusinessException.class, () -> request.requireAdmissibleOn(TODAY));
		assertDoesNotThrow(() -> request.requireAdmissibleOn(TODAY.plusDays(1)));
	}

	@Test
	void markCheckedIn_requiresApprovalAndCanOnlyHappenOnce() {
		VisitorRequest request = walkIn();
		assertThrows(BusinessException.class, request::markCheckedIn);

		request.approve(11L, TODAY);
		request.markCheckedIn();

		assertEquals(VisitorRequestStatus.CHECKED_IN, request.getStatus());
		assertThrows(BusinessException.class, request::markCheckedIn);
		assertThrows(BusinessException.class, request::cancel);
	}

	@Test
	void attachPhoto_beforeCheckIn_isAllowedButNotAfterARejection() {
		VisitorRequest request = walkIn();
		UUID photo = UUID.randomUUID();

		request.attachPhoto(photo);

		assertEquals(photo, request.getPhotoFilePublicId());
		request.reject(11L, "no");
		assertThrows(BusinessException.class, () -> request.attachPhoto(UUID.randomUUID()));
	}
}

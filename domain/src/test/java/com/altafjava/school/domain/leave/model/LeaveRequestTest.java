package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class LeaveRequestTest {

	private LeaveRequest submitted(LocalDate startDate, LocalDate endDate) {
		return LeaveRequest.submit(1L, 2L, 3L, startDate, endDate, "Family event", BigDecimal.valueOf(3), 1);
	}

	@Test
	void submit_storesProvidedDaysRequested() {
		LeaveRequest request = LeaveRequest.submit(1L, 2L, 3L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3),
				"Family event", BigDecimal.valueOf(2), 1);

		// daysRequested is caller-computed (LeaveDayCalculator, holiday-aware) — this entity just
		// persists whatever value it's given, it does not recompute from start/end itself.
		assertEquals(0, BigDecimal.valueOf(2).compareTo(request.getDaysRequested()));
		assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
	}

	@Test
	void submit_withEndDateBeforeStartDate_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> submitted(LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 1)));
	}

	@Test
	void approve_fromPending_setsApprovedStatus() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

		request.approve(99L);

		assertEquals(LeaveRequestStatus.APPROVED, request.getStatus());
		assertEquals(99L, request.getApprovedByUserId());
	}

	@Test
	void approve_whenNotPending_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
		request.approve(99L);

		assertThrows(BusinessException.class, () -> request.approve(99L));
	}

	@Test
	void reject_fromPending_setsRejectedStatusWithReason() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

		request.reject(99L, "Insufficient staffing on those dates");

		assertEquals(LeaveRequestStatus.REJECTED, request.getStatus());
		assertEquals("Insufficient staffing on those dates", request.getRejectionReason());
	}

	@Test
	void cancel_whenPendingAndFuture_setsCancelledStatus() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(5), LocalDate.now().plusDays(6));

		request.cancel();

		assertEquals(LeaveRequestStatus.CANCELLED, request.getStatus());
	}

	@Test
	void cancel_whenAlreadyRejected_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
		request.reject(99L, "No");

		assertThrows(BusinessException.class, request::cancel);
	}

	@Test
	void cancel_whenAlreadyStarted_throwsBusinessException() {
		LeaveRequest request = submitted(LocalDate.now().minusDays(3), LocalDate.now().plusDays(1));

		assertThrows(BusinessException.class, request::cancel);
	}

	private LeaveRequest twoLevel() {
		return LeaveRequest.submit(1L, 2L, 3L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), "Trip",
				BigDecimal.valueOf(2), 2);
	}

	@Test
	void awaitingStage_singleLevelRequest_isTheAdministrator() {
		LeaveRequest request = submitted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

		assertEquals(Optional.of(LeaveApprovalStage.ADMINISTRATOR), request.awaitingStage());
	}

	@Test
	void awaitingStage_twoLevelRequest_startsWithTheDepartmentHeadThenTheAdministrator() {
		LeaveRequest request = twoLevel();
		assertEquals(Optional.of(LeaveApprovalStage.DEPARTMENT_HEAD), request.awaitingStage());

		request.approve(7L);

		assertEquals(Optional.of(LeaveApprovalStage.ADMINISTRATOR), request.awaitingStage());
	}

	@Test
	void approve_firstOfTwoLevels_leavesTheRequestPending() {
		LeaveRequest request = twoLevel();

		request.approve(7L);

		assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
		assertEquals(1, request.getApprovalsGranted());
		assertEquals(null, request.getApprovedByUserId());
	}

	@Test
	void approve_finalLevel_approvesAndRecordsTheFinalApprover() {
		LeaveRequest request = twoLevel();
		request.approve(7L);

		request.approve(8L);

		assertEquals(LeaveRequestStatus.APPROVED, request.getStatus());
		assertEquals(8L, request.getApprovedByUserId());
		assertTrue(request.awaitingStage().isEmpty());
	}

	@Test
	void reject_atTheFirstLevel_endsTheRequest() {
		LeaveRequest request = twoLevel();

		request.reject(7L, "Exam week");

		assertEquals(LeaveRequestStatus.REJECTED, request.getStatus());
		assertTrue(request.awaitingStage().isEmpty());
	}
}

package com.altafjava.school.domain.leave.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StudentLeaveRequestTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 2);

	@Test
	void newRequest_isPending() {
		assertEquals(LeaveRequestStatus.PENDING, request().getStatus());
	}

	@Test
	void endBeforeStart_isRefused() {
		assertThrows(BusinessException.class,
				() -> StudentLeaveRequest.submit(1L, 2L, 3L, START, START.minusDays(1), "Fever", null));
	}

	@Test
	void approval_recordsWhoDecidedAndWhen() {
		StudentLeaveRequest request = request();

		request.approve(9L, LocalDateTime.now());

		assertEquals(LeaveRequestStatus.APPROVED, request.getStatus());
		assertEquals(9L, request.getDecidedByUserId());
		assertNotNull(request.getDecidedAt());
	}

	@Test
	void rejection_keepsTheReason() {
		StudentLeaveRequest request = request();

		request.reject(9L, "Exam week", LocalDateTime.now());

		assertEquals(LeaveRequestStatus.REJECTED, request.getStatus());
		assertEquals("Exam week", request.getRejectionReason());
	}

	@Test
	void decidedRequest_cannotChangeAgain() {
		StudentLeaveRequest request = request();
		request.approve(9L, LocalDateTime.now());

		assertThrows(BusinessException.class, () -> request.reject(9L, "Changed my mind", LocalDateTime.now()));
		assertThrows(BusinessException.class, request::cancel);
	}

	private static StudentLeaveRequest request() {
		return StudentLeaveRequest.submit(1L, 2L, 3L, START, START.plusDays(1), "Fever", null);
	}
}

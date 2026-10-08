package com.altafjava.school.domain.visitor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class VisitorLogTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);
	private static final UUID PHOTO = UUID.randomUUID();

	private VisitorLog checkedIn(LocalDateTime at) {
		VisitorRequest request = VisitorRequest.walkIn("Alex Ray", "555-0100", "Parent-teacher meeting", 7L, 3L,
				TODAY);
		request.setId(40L);
		return VisitorLog.checkIn(request, PHOTO, at);
	}

	@Test
	void checkIn_copiesTheRequestAndLeavesCheckOutNull() {
		LocalDateTime checkInAt = LocalDateTime.of(2026, 5, 1, 9, 0);

		VisitorLog log = checkedIn(checkInAt);

		assertEquals("Alex Ray", log.getVisitorName());
		assertEquals("555-0100", log.getVisitorPhone());
		assertEquals("Parent-teacher meeting", log.getPurpose());
		assertEquals(7L, log.getHostEmployeeId());
		assertEquals(40L, log.getVisitorRequestId());
		assertEquals(PHOTO, log.getPhotoFilePublicId());
		assertEquals(checkInAt, log.getCheckInAt());
		assertNull(log.getCheckOutAt());
		assertNull(log.getBadgeIssuanceId());
	}

	@Test
	void attachBadge_recordsTheIssuedBadge() {
		VisitorLog log = checkedIn(LocalDateTime.of(2026, 5, 1, 9, 0));

		log.attachBadge(99L);

		assertEquals(99L, log.getBadgeIssuanceId());
	}

	@Test
	void checkOut_setsCheckOutAt() {
		VisitorLog log = checkedIn(LocalDateTime.of(2026, 5, 1, 9, 0));

		LocalDateTime checkOutAt = LocalDateTime.of(2026, 5, 1, 9, 45);
		log.checkOut(checkOutAt);

		assertEquals(checkOutAt, log.getCheckOutAt());
	}

	@Test
	void checkOut_alreadyCheckedOut_throwsBusinessException() {
		VisitorLog log = checkedIn(LocalDateTime.of(2026, 5, 1, 9, 0));
		log.checkOut(LocalDateTime.of(2026, 5, 1, 9, 45));

		assertThrows(BusinessException.class, () -> log.checkOut(LocalDateTime.of(2026, 5, 1, 10, 0)));
	}

	@Test
	void checkOut_beforeCheckIn_throwsBusinessException() {
		VisitorLog log = checkedIn(LocalDateTime.of(2026, 5, 1, 9, 0));

		assertThrows(BusinessException.class, () -> log.checkOut(LocalDateTime.of(2026, 5, 1, 8, 0)));
	}
}

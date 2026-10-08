package com.altafjava.school.domain.library.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class BookReservationTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	private BookReservation queued() {
		return BookReservation.queue(1L, 2L);
	}

	private BookReservation ready() {
		BookReservation reservation = queued();
		reservation.hold(9L, TODAY.plusDays(3));
		return reservation;
	}

	@Test
	void queue_startsWaitingWithNoCopy() {
		BookReservation reservation = queued();

		assertEquals(ReservationStatus.QUEUED, reservation.getStatus());
		assertTrue(reservation.isLive());
		assertEquals(null, reservation.getHeldCopyId());
	}

	@Test
	void hold_setsAsideTheCopyUntilTheExpiryDate() {
		BookReservation reservation = ready();

		assertEquals(ReservationStatus.READY, reservation.getStatus());
		assertEquals(9L, reservation.getHeldCopyId());
		assertEquals(TODAY.plusDays(3), reservation.getHoldExpiresOn());
	}

	@Test
	void hold_whenNotWaiting_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> ready().hold(10L, TODAY));
	}

	@Test
	void holdHasLapsed_isTrueOnlyAfterTheExpiryDate() {
		BookReservation reservation = ready();

		assertFalse(reservation.holdHasLapsed(TODAY.plusDays(3)));
		assertTrue(reservation.holdHasLapsed(TODAY.plusDays(4)));
		assertFalse(queued().holdHasLapsed(TODAY.plusDays(40)));
	}

	@Test
	void fulfil_aReadyReservation_closesIt() {
		BookReservation reservation = ready();

		reservation.fulfil();

		assertEquals(ReservationStatus.FULFILLED, reservation.getStatus());
		assertFalse(reservation.isLive());
		assertNotNull(reservation.getClosedAt());
	}

	@Test
	void fulfil_aReservationStillWaiting_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> queued().fulfil());
	}

	@Test
	void cancel_aWaitingOrReadyReservation_closesIt() {
		BookReservation waiting = queued();
		BookReservation holding = ready();

		waiting.cancel();
		holding.cancel();

		assertEquals(ReservationStatus.CANCELLED, waiting.getStatus());
		assertEquals(ReservationStatus.CANCELLED, holding.getStatus());
	}

	@Test
	void cancel_aClosedReservation_throwsBusinessException() {
		BookReservation reservation = ready();
		reservation.fulfil();

		assertThrows(BusinessException.class, reservation::cancel);
	}

	@Test
	void expire_aReadyReservation_closesIt() {
		BookReservation reservation = ready();

		reservation.expire();

		assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
	}

	@Test
	void expire_aReservationStillWaiting_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> queued().expire());
	}
}

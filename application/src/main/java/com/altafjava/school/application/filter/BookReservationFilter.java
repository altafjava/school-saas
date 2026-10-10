package com.altafjava.school.application.filter;

import com.altafjava.school.domain.library.model.ReservationStatus;

/** Narrows the reservation list; every part is optional. */
public record BookReservationFilter(String bookPublicId, String studentPublicId, ReservationStatus status) {

	public static final BookReservationFilter NONE = new BookReservationFilter(null, null, null);
}

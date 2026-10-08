package com.altafjava.school.domain.timetable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class VenueTest {

	@Test
	void create_startsActive() {
		Venue venue = Venue.create("LAB-1", "Physics Lab", VenueType.LABORATORY, 30);

		assertTrue(venue.isActive());
		assertEquals(30, venue.getCapacity());
	}

	@Test
	void create_withoutCapacity_isAllowed() {
		assertEquals(null, Venue.create("HALL", "Main Hall", VenueType.HALL, null).getCapacity());
	}

	@Test
	void create_withNonPositiveCapacity_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> Venue.create("X", "X", VenueType.OTHER, 0));
		assertThrows(BusinessException.class, () -> Venue.create("X", "X", VenueType.OTHER, -5));
	}

	@Test
	void updateDetails_changesNameTypeAndCapacity() {
		Venue venue = Venue.create("R-1", "Room 1", VenueType.CLASSROOM, 40);

		venue.updateDetails("Room One", VenueType.LIBRARY, 25);

		assertEquals("Room One", venue.getName());
		assertEquals(VenueType.LIBRARY, venue.getVenueType());
		assertEquals(25, venue.getCapacity());
	}

	@Test
	void deactivateThenActivate_togglesAvailability() {
		Venue venue = Venue.create("R-1", "Room 1", VenueType.CLASSROOM, 40);

		venue.deactivate();
		assertFalse(venue.isActive());

		venue.activate();
		assertTrue(venue.isActive());
	}
}

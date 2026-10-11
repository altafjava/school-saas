package com.altafjava.school.application.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateWindowTest {

	@Test
	void bounds_coverWholeDaysAtBothEnds() {
		DateWindow window = new DateWindow(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

		assertEquals(LocalDateTime.of(2026, 3, 1, 0, 0), window.startInclusive());
		assertEquals(LocalDateTime.of(2026, 4, 1, 0, 0), window.endExclusive());
	}

	@Test
	void bounds_ofAnOpenWindow_areNull() {
		assertNull(DateWindow.UNBOUNDED.startInclusive());
		assertNull(DateWindow.UNBOUNDED.endExclusive());
	}

	@Test
	void construct_withFromAfterTo_isRejected() {
		LocalDate from = LocalDate.of(2026, 3, 2);
		LocalDate to = LocalDate.of(2026, 3, 1);

		assertThrows(IllegalArgumentException.class, () -> new DateWindow(from, to));
	}

	@Test
	void construct_withSameDay_isAccepted() {
		LocalDate day = LocalDate.of(2026, 3, 1);

		assertEquals(LocalDateTime.of(2026, 3, 2, 0, 0), new DateWindow(day, day).endExclusive());
	}
}

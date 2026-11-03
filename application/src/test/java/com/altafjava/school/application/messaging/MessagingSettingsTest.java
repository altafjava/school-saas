package com.altafjava.school.application.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class MessagingSettingsTest {

	@Test
	void noHours_isOpenAllDay() {
		assertTrue(new MessagingSettings(true, null, null).isOpenAt(LocalTime.MIDNIGHT));
	}

	@Test
	void dayWindow_includesStartAndExcludesEnd() {
		MessagingSettings settings = new MessagingSettings(true, LocalTime.of(8, 0), LocalTime.of(18, 0));

		assertTrue(settings.isOpenAt(LocalTime.of(8, 0)));
		assertFalse(settings.isOpenAt(LocalTime.of(18, 0)));
		assertFalse(settings.isOpenAt(LocalTime.of(7, 59)));
	}

	@Test
	void windowCanCrossMidnight() {
		MessagingSettings settings = new MessagingSettings(true, LocalTime.of(22, 0), LocalTime.of(6, 0));

		assertTrue(settings.isOpenAt(LocalTime.of(23, 0)));
		assertTrue(settings.isOpenAt(LocalTime.of(5, 0)));
		assertFalse(settings.isOpenAt(LocalTime.of(12, 0)));
	}
}

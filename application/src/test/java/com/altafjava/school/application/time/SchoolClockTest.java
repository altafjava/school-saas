package com.altafjava.school.application.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SchoolClockTest {

	private static final Long TENANT_ID = 1L;
	// 19:00 UTC on 31 March is already 1 April in India.
	private static final Clock LATE_EVENING_UTC = Clock.fixed(Instant.parse("2026-03-31T19:00:00Z"), ZoneOffset.UTC);
	private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

	private final SchoolClock clock = TestSchoolClocks.fixed(LATE_EVENING_UTC, INDIA);

	@Test
	void today_isTheSchoolsDate_notTheServers() {
		assertEquals(LocalDate.of(2026, 4, 1), clock.today(TENANT_ID));
	}

	@Test
	void now_isTheSchoolsWallClock() {
		assertEquals(LocalDateTime.of(2026, 4, 1, 0, 30), clock.now(TENANT_ID));
	}

	@Test
	void currentMonth_followsTheSchoolsDate() {
		assertEquals(YearMonth.of(2026, 4), clock.currentMonth(TENANT_ID));
	}
}

package com.altafjava.school.domain.rollup.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttendanceRollupTest {

	@Test
	void total_sumsAllFourStatuses() {
		AttendanceRollup rollup = new AttendanceRollup(10, 0, 2, 1, 3);

		assertEquals(16, rollup.total());
	}

	@Test
	void sum_combinesMultipleCampusesFieldByField() {
		AttendanceRollup campusA = new AttendanceRollup(10, 0, 2, 1, 3);
		AttendanceRollup campusB = new AttendanceRollup(20, 0, 0, 4, 1);

		AttendanceRollup total = AttendanceRollup.sum(List.of(campusA, campusB));

		assertEquals(new AttendanceRollup(30, 0, 2, 5, 4), total);
	}

	@Test
	void sum_emptyList_returnsZero() {
		AttendanceRollup total = AttendanceRollup.sum(List.of());

		assertEquals(AttendanceRollup.ZERO, total);
	}
}

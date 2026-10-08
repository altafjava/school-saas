package com.altafjava.school.domain.timetable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class TimetableSubstitutionTest {

	// 2026-10-12 is a Monday.
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
	private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

	private TimetableEntry mondaySlotTaughtBy(long teacherId) {
		TimetableEntry entry = TimetableEntry.create(DayOfWeek.MONDAY, 1L, 2L, 3L, teacherId, null);
		entry.setId(50L);
		return entry;
	}

	@Test
	void assign_onTheSlotsWeekday_coversThatSlotAndPeriod() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 9L,
				"Sick leave", 77L, TODAY);

		assertEquals(50L, substitution.getTimetableEntryId());
		assertEquals(1L, substitution.getPeriodId());
		assertEquals(9L, substitution.getSubstituteTeacherId());
		assertTrue(substitution.isActive());
	}

	@Test
	void assign_onADifferentWeekday_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> TimetableSubstitution.assign(mondaySlotTaughtBy(4L),
				MONDAY.plusDays(1), 9L, null, 77L, TODAY));
	}

	@Test
	void assign_forAPastDate_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> TimetableSubstitution.assign(mondaySlotTaughtBy(4L),
				LocalDate.of(2026, 10, 5), 9L, null, 77L, TODAY));
	}

	@Test
	void assign_theRegularTeacherAsTheirOwnSubstitute_throwsBusinessException() {
		assertThrows(BusinessException.class,
				() -> TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 4L, null, 77L, TODAY));
	}

	@Test
	void cancel_beforeTheDate_marksItCancelledWithAReason() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 9L, null,
				77L, TODAY);

		substitution.cancel("Teacher returned", TODAY);

		assertFalse(substitution.isActive());
		assertEquals("Teacher returned", substitution.getCancellationReason());
	}

	@Test
	void cancel_twice_throwsBusinessException() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 9L, null,
				77L, TODAY);
		substitution.cancel("x", TODAY);

		assertThrows(BusinessException.class, () -> substitution.cancel("y", TODAY));
	}

	@Test
	void cancel_afterTheDateHasPassed_throwsBusinessException() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 9L, null,
				77L, TODAY);

		assertThrows(BusinessException.class, () -> substitution.cancel("late", MONDAY.plusDays(1)));
	}

	@Test
	void cancel_onTheDayItself_isStillAllowed() {
		TimetableSubstitution substitution = TimetableSubstitution.assign(mondaySlotTaughtBy(4L), MONDAY, 9L, null,
				77L, TODAY);

		substitution.cancel("cover no longer needed", MONDAY);

		assertFalse(substitution.isActive());
	}
}

package com.altafjava.school.domain.exam.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class ExamTest {

	private Exam newExam() {
		return Exam.create("Midterm", 5L, 10L, new ExamSlot(LocalDateTime.now().plusDays(7), 60, null),
				BigDecimal.valueOf(100), null, 1L,
				Exam.FULL_WEIGHTAGE);
	}

	@Test
	void create_defaultsStatusToScheduled() {
		Exam exam = newExam();

		assertEquals(ExamStatus.SCHEDULED, exam.getStatus());
	}

	@Test
	void complete_scheduledExam_transitionsToCompleted() {
		Exam exam = newExam();

		exam.complete();

		assertEquals(ExamStatus.COMPLETED, exam.getStatus());
	}

	@Test
	void complete_cancelledExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.cancel();

		assertThrows(BusinessException.class, exam::complete);
	}

	@Test
	void complete_alreadyCompletedExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class, exam::complete);
	}

	@Test
	void cancel_scheduledExam_transitionsToCancelled() {
		Exam exam = newExam();

		exam.cancel();

		assertEquals(ExamStatus.CANCELLED, exam.getStatus());
	}

	@Test
	void cancel_completedExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class, exam::cancel);
	}

	@Test
	void cancel_alreadyCancelledExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.cancel();

		assertThrows(BusinessException.class, exam::cancel);
	}

	private Exam completedExam() {
		Exam exam = newExam();
		exam.complete();
		return exam;
	}

	@Test
	void create_withWeightageOutsideTheValidRange_throwsBusinessException() {
		LocalDateTime when = LocalDateTime.now().plusDays(7);

		assertThrows(BusinessException.class,
				() -> Exam.create("T", 5L, 10L, new ExamSlot(when, 60, null), BigDecimal.TEN, null, 1L,
						BigDecimal.ZERO));
		assertThrows(BusinessException.class,
				() -> Exam.create("T", 5L, 10L, new ExamSlot(when, 60, null), BigDecimal.TEN, null, 1L,
						new BigDecimal("100.01")));
		assertThrows(BusinessException.class,
				() -> Exam.create("T", 5L, 10L, new ExamSlot(when, 60, null), BigDecimal.TEN, null, 1L, null));
	}

	@Test
	void create_startsWithResultsUnpublished() {
		assertFalse(newExam().isResultsPublished());
	}

	@Test
	void publishResults_completedExam_recordsWhenAndByWhom() {
		Exam exam = completedExam();

		exam.verifyMarks("checker");
		exam.lockMarks("registrar");
		exam.publishResults("registrar");

		assertTrue(exam.isResultsPublished());
		assertEquals("registrar", exam.getResultsPublishedBy());
	}

	@Test
	void publishResults_exceptWhenCompleted_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> newExam().publishResults("registrar"));
	}

	@Test
	void publishResults_twice_throwsBusinessException() {
		Exam exam = completedExam();
		exam.verifyMarks("checker");
		exam.lockMarks("registrar");
		exam.publishResults("registrar");

		assertThrows(BusinessException.class, () -> exam.publishResults("registrar"));
	}

	@Test
	void withdrawResults_publishedExam_hidesThemAgain() {
		Exam exam = completedExam();
		exam.verifyMarks("checker");
		exam.lockMarks("registrar");
		exam.publishResults("registrar");

		exam.withdrawResults();

		assertFalse(exam.isResultsPublished());
		assertEquals(null, exam.getResultsPublishedBy());
	}

	@Test
	void withdrawResults_unpublishedExam_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> completedExam().withdrawResults());
	}

	@Test
	void reweight_beforePublication_changesWeightage() {
		Exam exam = newExam();

		exam.reweight(new BigDecimal("25"));

		assertEquals(0, new BigDecimal("25").compareTo(exam.getWeightage()));
	}

	@Test
	void reweight_afterPublication_throwsBusinessException() {
		Exam exam = completedExam();
		exam.verifyMarks("checker");
		exam.lockMarks("registrar");
		exam.publishResults("registrar");

		assertThrows(BusinessException.class, () -> exam.reweight(new BigDecimal("25")));
	}

	// ---- exam timetable ----

	@Test
	void clashesWith_overlappingExamOfTheSameClassroom_isAClash() {
		Exam first = slotted(10L, null, 9, 0, 120);
		Exam second = slotted(10L, null, 10, 30, 60);

		assertTrue(first.clashesWith(second));
		assertTrue(second.clashesWith(first));
	}

	@Test
	void clashesWith_backToBackExams_doNotClash() {
		assertFalse(slotted(10L, null, 9, 0, 60).clashesWith(slotted(10L, null, 10, 0, 60)));
	}

	@Test
	void clashesWith_otherClassroomInTheSameVenue_isAClash() {
		assertTrue(slotted(10L, 7L, 9, 0, 60).clashesWith(slotted(11L, 7L, 9, 30, 60)));
	}

	@Test
	void clashesWith_otherClassroomWithoutASharedVenue_doesNotClash() {
		assertFalse(slotted(10L, 7L, 9, 0, 60).clashesWith(slotted(11L, 8L, 9, 0, 60)));
		assertFalse(slotted(10L, null, 9, 0, 60).clashesWith(slotted(11L, null, 9, 0, 60)));
	}

	@Test
	void clashesWith_aCancelledExam_doesNotClash() {
		Exam cancelled = slotted(10L, null, 9, 0, 60);
		cancelled.cancel();

		assertFalse(slotted(10L, null, 9, 0, 60).clashesWith(cancelled));
	}

	@Test
	void create_withADurationOutsideTheLimits_throwsBusinessException() {
		LocalDateTime when = LocalDateTime.of(2026, 3, 2, 9, 0);
		assertThrows(BusinessException.class, () -> Exam.create("T", 5L, 10L, new ExamSlot(when, 4, null),
				BigDecimal.TEN, null, 1L, BigDecimal.TEN));
		assertThrows(BusinessException.class, () -> Exam.create("T", 5L, 10L, new ExamSlot(when, 721, null),
				BigDecimal.TEN, null, 1L, BigDecimal.TEN));
	}

	@Test
	void publishSchedule_onlyOnce_andOnlyWhileScheduled() {
		Exam exam = newExam();
		exam.publishSchedule();

		assertTrue(exam.isSchedulePublished());
		assertThrows(BusinessException.class, exam::publishSchedule);
		Exam completed = newExam();
		completed.complete();
		assertThrows(BusinessException.class, completed::publishSchedule);
	}

	@Test
	void reschedule_aCompletedExam_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class,
				() -> exam.reschedule(new ExamSlot(LocalDateTime.of(2026, 3, 2, 9, 0), 60, null)));
	}

	// ---- marks verification ----

	@Test
	void marks_moveFromEnteredToVerifiedToLocked() {
		Exam exam = newExam();
		exam.complete();

		exam.verifyMarks("checker");
		assertEquals(MarksStage.VERIFIED, exam.getMarksStage());
		assertEquals("checker", exam.getMarksVerifiedBy());
		exam.lockMarks("registrar");
		assertEquals(MarksStage.LOCKED, exam.getMarksStage());
		assertEquals("registrar", exam.getMarksLockedBy());
	}

	@Test
	void verifyMarks_beforeTheExamIsCompleted_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> newExam().verifyMarks("checker"));
	}

	@Test
	void requireMarksOpen_onceVerified_refusesAnyChange() {
		Exam exam = newExam();
		exam.complete();
		exam.requireMarksOpen();
		exam.verifyMarks("checker");

		assertThrows(BusinessException.class, exam::requireMarksOpen);
	}

	@Test
	void returnMarksForCorrection_reopensThemAndClearsTheVerification() {
		Exam exam = newExam();
		exam.complete();
		exam.verifyMarks("checker");

		exam.returnMarksForCorrection();

		assertEquals(MarksStage.ENTERED, exam.getMarksStage());
		assertNull(exam.getMarksVerifiedBy());
	}

	@Test
	void lockMarks_withoutVerification_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();

		assertThrows(BusinessException.class, () -> exam.lockMarks("registrar"));
	}

	@Test
	void unlockMarks_whileResultsArePublished_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();
		exam.verifyMarks("checker");
		exam.lockMarks("registrar");
		exam.publishResults("registrar");

		assertThrows(BusinessException.class, exam::unlockMarks);
		exam.withdrawResults();
		exam.unlockMarks();
		assertEquals(MarksStage.ENTERED, exam.getMarksStage());
	}

	@Test
	void publishResults_beforeTheMarksAreLocked_throwsBusinessException() {
		Exam exam = newExam();
		exam.complete();
		exam.verifyMarks("checker");

		assertThrows(BusinessException.class, () -> exam.publishResults("registrar"));
	}

	private static Exam slotted(Long classroomId, Long venueId, int hour, int minute, int durationMinutes) {
		return Exam.create("T", 5L, classroomId,
				new ExamSlot(LocalDateTime.of(2026, 3, 2, hour, minute), durationMinutes, venueId), BigDecimal.TEN,
				null, 1L, BigDecimal.TEN);
	}
}

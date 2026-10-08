package com.altafjava.school.domain.exam.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class ExamTest {

	private Exam newExam() {
		return Exam.create("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), null, 1L,
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
				() -> Exam.create("T", 5L, 10L, when, BigDecimal.TEN, null, 1L, BigDecimal.ZERO));
		assertThrows(BusinessException.class,
				() -> Exam.create("T", 5L, 10L, when, BigDecimal.TEN, null, 1L, new BigDecimal("100.01")));
		assertThrows(BusinessException.class,
				() -> Exam.create("T", 5L, 10L, when, BigDecimal.TEN, null, 1L, null));
	}

	@Test
	void create_startsWithResultsUnpublished() {
		assertFalse(newExam().isResultsPublished());
	}

	@Test
	void publishResults_completedExam_recordsWhenAndByWhom() {
		Exam exam = completedExam();

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
		exam.publishResults("registrar");

		assertThrows(BusinessException.class, () -> exam.publishResults("registrar"));
	}

	@Test
	void withdrawResults_publishedExam_hidesThemAgain() {
		Exam exam = completedExam();
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
		exam.publishResults("registrar");

		assertThrows(BusinessException.class, () -> exam.reweight(new BigDecimal("25")));
	}
}

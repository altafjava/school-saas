package com.altafjava.school.domain.lms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class SubmissionTest {

	@Test
	void submit_onTime_setsStatusSubmitted() {
		Submission submission = Submission.submit(1L, 2L, "tenants/1/submissions/key.pdf", "My answer", false,
				LocalDateTime.now());

		assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());
		assertEquals(1L, submission.getAssignmentId());
		assertEquals(2L, submission.getStudentId());
		assertNotNull(submission.getSubmittedAt());
	}

	@Test
	void submit_late_setsStatusLate() {
		Submission submission = Submission.submit(1L, 2L, null, "My answer", true, LocalDateTime.now());

		assertEquals(SubmissionStatus.LATE, submission.getStatus());
	}

	@Test
	void grade_setsMarksFeedbackAndGradedStatus() {
		Submission submission = Submission.submit(1L, 2L, null, "My answer", false, LocalDateTime.now());

		submission.grade(BigDecimal.valueOf(85), "Good work", 7L, LocalDateTime.now());

		assertEquals(SubmissionStatus.GRADED, submission.getStatus());
		assertEquals(0, BigDecimal.valueOf(85).compareTo(submission.getMarksObtained()));
		assertEquals("Good work", submission.getFeedback());
		assertEquals(7L, submission.getGradedBy());
		assertNotNull(submission.getGradedAt());
	}

	@Test
	void grade_alreadyGraded_throwsBusinessException() {
		Submission submission = Submission.submit(1L, 2L, null, "My answer", false, LocalDateTime.now());
		submission.grade(BigDecimal.valueOf(85), "Good work", 7L, LocalDateTime.now());

		assertThrows(BusinessException.class,
				() -> submission.grade(BigDecimal.valueOf(90), "Regraded", 7L, LocalDateTime.now()));
	}
}

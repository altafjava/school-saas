package com.altafjava.school.domain.admission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class AdmissionTest {

	private Admission submitted() {
		return Admission.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith", "bob@family.test",
				"555-1234", "Grade 3");
	}

	@Test
	void submit_setsFieldsAndDefaultsToSubmittedStatus() {
		Admission admission = submitted();

		assertEquals("Alice", admission.getApplicantFirstName());
		assertEquals("Smith", admission.getApplicantLastName());
		assertEquals("Bob", admission.getGuardianFirstName());
		assertEquals("Grade 3", admission.getAppliedGrade());
		assertEquals(AdmissionStatus.SUBMITTED, admission.getStatus());
	}

	@Test
	void markUnderReview_transitionsStatus() {
		Admission admission = submitted();

		admission.markUnderReview();

		assertEquals(AdmissionStatus.UNDER_REVIEW, admission.getStatus());
	}

	@Test
	void markUnderReview_fromNonSubmitted_throwsBusinessException() {
		Admission admission = submitted();
		admission.markUnderReview();

		assertThrows(BusinessException.class, admission::markUnderReview);
	}

	@Test
	void approve_transitionsStatus() {
		Admission admission = submitted();

		admission.approve();

		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
	}

	@Test
	void approve_afterAlreadyApproved_throwsBusinessException() {
		Admission admission = submitted();
		admission.approve();

		assertThrows(BusinessException.class, admission::approve);
	}

	@Test
	void approve_afterRejected_throwsBusinessException() {
		Admission admission = submitted();
		admission.reject();

		assertThrows(BusinessException.class, admission::approve);
	}

	@Test
	void reject_transitionsStatus() {
		Admission admission = submitted();

		admission.reject();

		assertEquals(AdmissionStatus.REJECTED, admission.getStatus());
	}

	@Test
	void reject_afterAlreadyEnrolled_throwsBusinessException() {
		Admission admission = submitted();
		admission.approve();
		admission.markEnrolled();

		assertThrows(BusinessException.class, admission::reject);
	}

	@Test
	void enrollmentLifecycle_tracksSagaAndCreatedRecords() {
		Admission admission = submitted();
		admission.approve();
		UUID sagaId = UUID.randomUUID();

		admission.beginEnrollmentSaga(sagaId);
		admission.recordEnrolledStudent(10L);
		admission.recordEnrolledGuardian(20L);
		admission.markEnrolled();

		assertEquals(sagaId, admission.getEnrollmentSagaId());
		assertEquals(10L, admission.getEnrolledStudentId());
		assertEquals(20L, admission.getEnrolledGuardianId());
		assertEquals(AdmissionStatus.ENROLLED, admission.getStatus());
	}

	@Test
	void markEnrolled_fromNonApproved_throwsBusinessException() {
		Admission admission = submitted();

		assertThrows(BusinessException.class, admission::markEnrolled);
	}

	@Test
	void revertEnrollment_clearsTrackingFieldsAndRestoresApprovedStatus() {
		Admission admission = submitted();
		admission.approve();
		admission.beginEnrollmentSaga(UUID.randomUUID());
		admission.recordEnrolledStudent(10L);
		admission.recordEnrolledGuardian(20L);
		admission.markEnrolled();

		admission.revertEnrollment();

		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
		assertNull(admission.getEnrolledStudentId());
		assertNull(admission.getEnrolledGuardianId());
		assertNull(admission.getEnrollmentSagaId());
	}

	@Test
	void recordEntranceTestScore_fromUnderReview_setsScoreFields() {
		Admission admission = submitted();
		admission.markUnderReview();

		admission.recordEntranceTestScore(BigDecimal.valueOf(88), BigDecimal.valueOf(100));

		assertEquals(BigDecimal.valueOf(88), admission.getEntranceTestScore());
		assertEquals(BigDecimal.valueOf(100), admission.getEntranceTestMaxScore());
	}

	@Test
	void recordEntranceTestScore_fromSubmitted_throwsBusinessException() {
		Admission admission = submitted();

		assertThrows(BusinessException.class,
				() -> admission.recordEntranceTestScore(BigDecimal.valueOf(88), BigDecimal.valueOf(100)));
	}

	@Test
	void assignMeritRank_setsRank() {
		Admission admission = submitted();

		admission.assignMeritRank(3);

		assertEquals(3, admission.getMeritRank());
	}

	@Test
	void assignMeritRank_withNonPositiveRank_throwsBusinessException() {
		Admission admission = submitted();

		assertThrows(BusinessException.class, () -> admission.assignMeritRank(0));
	}

	@Test
	void waitlist_fromUnderReview_transitionsStatus() {
		Admission admission = submitted();
		admission.markUnderReview();

		admission.waitlist();

		assertEquals(AdmissionStatus.WAITLISTED, admission.getStatus());
	}

	@Test
	void waitlist_fromSubmitted_throwsBusinessException() {
		Admission admission = submitted();

		assertThrows(BusinessException.class, admission::waitlist);
	}

	@Test
	void promoteFromWaitlist_fromWaitlisted_transitionsToUnderReview() {
		Admission admission = submitted();
		admission.markUnderReview();
		admission.waitlist();

		admission.promoteFromWaitlist();

		assertEquals(AdmissionStatus.UNDER_REVIEW, admission.getStatus());
	}

	@Test
	void promoteFromWaitlist_fromUnderReview_throwsBusinessException() {
		Admission admission = submitted();
		admission.markUnderReview();

		assertThrows(BusinessException.class, admission::promoteFromWaitlist);
	}
}

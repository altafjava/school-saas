package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.EmailService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.saga.AdmissionEnrollmentSaga;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.admission.model.AdmissionDecision;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.admission.model.DecisionOutcome;
import com.altafjava.school.domain.admission.repository.AdmissionDecisionRepository;
import com.altafjava.school.domain.admission.repository.AdmissionRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

	@Mock
	private AdmissionRepository admissionRepository;
	@Mock
	private AdmissionDecisionRepository admissionDecisionRepository;
	@Mock
	private AdmissionEnrollmentSaga admissionEnrollmentSaga;
	@Mock
	private EmailService emailService;

	private AdmissionService admissionService;

	@BeforeEach
	void setUp() {
		admissionService = new AdmissionService(admissionRepository, admissionDecisionRepository,
				admissionEnrollmentSaga, emailService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Admission admissionWithId(long id, UUID publicId, AdmissionStatus status) {
		Admission admission = Admission.builder()
				.applicantFirstName("Alice")
				.applicantLastName("Smith")
				.applicantDateOfBirth(LocalDate.of(2015, 1, 1))
				.guardianFirstName("Bob")
				.guardianLastName("Smith")
				.guardianEmail("bob@family.test")
				.guardianPhone("555-1234")
				.appliedGrade("Grade 3")
				.status(status)
				.submittedAt(Instant.now())
				.build();
		admission.setId(id);
		admission.setPublicId(publicId);
		return admission;
	}

	@Test
	void submit_createsAdmissionWithSubmittedStatus() {
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission admission = admissionService.submit("Alice", "Smith", LocalDate.of(2015, 1, 1), "Bob", "Smith",
				"bob@family.test", "555-1234", "Grade 3");

		assertEquals(AdmissionStatus.SUBMITTED, admission.getStatus());
	}

	@Test
	void markUnderReview_fromSubmitted_succeeds() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.markUnderReview(publicId.toString());

		assertEquals(AdmissionStatus.UNDER_REVIEW, result.getStatus());
	}

	@Test
	void markUnderReview_fromApproved_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.APPROVED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.markUnderReview(publicId.toString()));

		verify(admissionRepository, never()).save(any());
	}

	@Test
	void requestApproval_withoutStudentCode_throwsBusinessException() {
		// Checked before any lookup — no admissionRepository stub needed, matching
		// AdmissionController's own boundary check for the same input.
		UUID publicId = UUID.randomUUID();

		assertThrows(BusinessException.class,
				() -> admissionService.requestApproval(publicId.toString(), "admin", null, null));

		verify(admissionEnrollmentSaga, never()).enroll(anyLong(), anyString());
	}

	@Test
	void requestApproval_alreadyEnrolled_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.ENROLLED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class,
				() -> admissionService.requestApproval(publicId.toString(), "admin", null, "STU-100"));

		verify(admissionDecisionRepository, never()).save(any());
	}

	// requestApproval's own body only runs when no ADMISSION_DECISION workflow is configured for
	// the tenant (ApprovalAspect's fallback) — this unit test constructs AdmissionService directly
	// with no AOP proxy in play, so it always exercises that fallback path, which finalizes
	// immediately exactly like AdmissionApprovalHandler does once a configured workflow approves.
	@Test
	void requestApproval_recordsDecisionApprovesAndTriggersSaga() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(admissionDecisionRepository.save(any(AdmissionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

		admissionService.requestApproval(publicId.toString(), "admin", "looks good", "STU-100");

		ArgumentCaptor<AdmissionDecision> decisionCaptor = ArgumentCaptor.forClass(AdmissionDecision.class);
		verify(admissionDecisionRepository).save(decisionCaptor.capture());
		assertEquals(DecisionOutcome.APPROVED, decisionCaptor.getValue().getOutcome());
		assertEquals(AdmissionStatus.APPROVED, admission.getStatus());
		verify(admissionEnrollmentSaga).enroll(eq(1L), eq("STU-100"));
	}

	@Test
	void reject_recordsDecisionAndDoesNotTriggerSaga() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(admissionDecisionRepository.save(any(AdmissionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.reject(publicId.toString(), "admin", "not a fit");

		assertEquals(AdmissionStatus.REJECTED, result.getStatus());
		verify(admissionEnrollmentSaga, never()).enroll(anyLong(), anyString());
	}

	@Test
	void reject_notifiesGuardianByEmail() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));
		when(admissionDecisionRepository.save(any(AdmissionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

		admissionService.reject(publicId.toString(), "admin", "not a fit");

		verify(emailService).sendEmail(eq("bob@family.test"), anyString(), anyString());
	}

	@Test
	void recordEntranceTestScore_fromUnderReview_succeeds() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.UNDER_REVIEW);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.recordEntranceTestScore(publicId.toString(), BigDecimal.valueOf(85),
				BigDecimal.valueOf(100));

		assertEquals(BigDecimal.valueOf(85), result.getEntranceTestScore());
		assertEquals(BigDecimal.valueOf(100), result.getEntranceTestMaxScore());
	}

	@Test
	void recordEntranceTestScore_fromSubmitted_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.SUBMITTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.recordEntranceTestScore(publicId.toString(),
				BigDecimal.valueOf(85), BigDecimal.valueOf(100)));

		verify(admissionRepository, never()).save(any());
	}

	@Test
	void generateMeritList_ranksDescendingAndWaitlistsBeyondAvailableSeats() {
		Admission low = admissionWithScore(1L, UUID.randomUUID(), BigDecimal.valueOf(60));
		Admission high = admissionWithScore(2L, UUID.randomUUID(), BigDecimal.valueOf(95));
		Admission mid = admissionWithScore(3L, UUID.randomUUID(), BigDecimal.valueOf(75));
		when(admissionRepository.findAllByTenantIdAndAppliedGradeAndStatusAndEntranceTestScoreIsNotNull(1L,
				"Grade 3", AdmissionStatus.UNDER_REVIEW)).thenReturn(new ArrayList<>(List.of(low, high, mid)));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		List<Admission> ranked = admissionService.generateMeritList("Grade 3", 2);

		assertEquals(3, ranked.size());
		assertEquals(1, high.getMeritRank());
		assertEquals(AdmissionStatus.UNDER_REVIEW, high.getStatus());
		assertEquals(2, mid.getMeritRank());
		assertEquals(AdmissionStatus.UNDER_REVIEW, mid.getStatus());
		assertEquals(3, low.getMeritRank());
		assertEquals(AdmissionStatus.WAITLISTED, low.getStatus());
	}

	@Test
	void promoteFromWaitlist_fromWaitlisted_succeeds() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.WAITLISTED);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));
		when(admissionRepository.save(any(Admission.class))).thenAnswer(inv -> inv.getArgument(0));

		Admission result = admissionService.promoteFromWaitlist(publicId.toString());

		assertEquals(AdmissionStatus.UNDER_REVIEW, result.getStatus());
	}

	@Test
	void promoteFromWaitlist_fromUnderReview_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Admission admission = admissionWithId(1L, publicId, AdmissionStatus.UNDER_REVIEW);
		when(admissionRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(admission));

		assertThrows(BusinessException.class, () -> admissionService.promoteFromWaitlist(publicId.toString()));

		verify(admissionRepository, never()).save(any());
	}

	private Admission admissionWithScore(long id, UUID publicId, BigDecimal score) {
		Admission admission = admissionWithId(id, publicId, AdmissionStatus.UNDER_REVIEW);
		admission.recordEntranceTestScore(score, BigDecimal.valueOf(100));
		return admission;
	}
}

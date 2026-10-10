package com.altafjava.school.application.service;

import static com.altafjava.school.application.support.TestEntities.publicId;
import static com.altafjava.school.application.support.TestEntities.withId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.exam.event.ExamResultsPublishedEvent;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

	@Mock
	private ExamRepository examRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private TermRepository termRepository;
	@Mock
	private ExamTypeDefinitionRepository examTypeDefinitionRepository;
	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private EventPublisher eventPublisher;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private PublicIdLookup publicIdLookup;

	private ExamService examService;

	@BeforeEach
	void setUp() {
		examService = new ExamService(examRepository, classroomRepository, subjectRepository, termRepository,
				examTypeDefinitionRepository, gradeRepository, eventPublisher, academicAccessGuard, publicIdLookup);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void schedule_withNonExistentClassroom_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", publicId("subject", 5).toString(), publicId("classroom", 99).toString(), LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), null, publicId("examType", 1).toString(), Exam.FULL_WEIGHTAGE));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withNonExistentSubject_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 10), 1L)).thenReturn(Optional.of(withId(Classroom.class, 10L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", publicId("subject", 99).toString(), publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), null, publicId("examType", 1).toString(), Exam.FULL_WEIGHTAGE));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withExistingClassroomAndSubject_succeeds() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 10), 1L)).thenReturn(Optional.of(withId(Classroom.class, 10L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 5), 1L)).thenReturn(Optional.of(withId(Subject.class, 5L)));
		when(examTypeDefinitionRepository.findByPublicIdAndTenantId(publicId("examType", 1), 1L)).thenReturn(Optional.of(withId(ExamTypeDefinition.class, 1L)));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		assertDoesNotThrow(() -> examService.schedule("Midterm", publicId("subject", 5).toString(), publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100), null, publicId("examType", 1).toString(), Exam.FULL_WEIGHTAGE));
	}

	@Test
	void schedule_withNonExistentTerm_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 10), 1L)).thenReturn(Optional.of(withId(Classroom.class, 10L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 5), 1L)).thenReturn(Optional.of(withId(Subject.class, 5L)));
		when(termRepository.findByPublicIdAndTenantId(publicId("term", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", publicId("subject", 5).toString(), publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), publicId("term", 99).toString(), publicId("examType", 1).toString(), Exam.FULL_WEIGHTAGE));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withNonExistentExamType_throwsResourceNotFound() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 10), 1L)).thenReturn(Optional.of(withId(Classroom.class, 10L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 5), 1L)).thenReturn(Optional.of(withId(Subject.class, 5L)));
		when(examTypeDefinitionRepository.findByPublicIdAndTenantId(publicId("examType", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> examService.schedule("Midterm", publicId("subject", 5).toString(), publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
						BigDecimal.valueOf(100), null, publicId("examType", 99).toString(), Exam.FULL_WEIGHTAGE));

		verify(examRepository, never()).save(any());
	}

	private Exam examWithPublicId(UUID publicId) {
		Exam exam = Exam.create("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), null,
				1L, Exam.FULL_WEIGHTAGE);
		exam.setPublicId(publicId);
		return exam;
	}

	@Test
	void reschedule_updatesScheduledAt() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		LocalDateTime newTime = LocalDateTime.now().plusDays(14);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam rescheduled = examService.reschedule(publicId.toString(), newTime, ExpectedVersion.any());

		assertEquals(newTime, rescheduled.getScheduledAt());
	}

	@Test
	void assignTerm_withExistingTerm_succeeds() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(termRepository.findByPublicIdAndTenantId(publicId("term", 7), 1L))
				.thenReturn(Optional.of(withId(Term.class, 7L)));
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, -1L))
				.thenReturn(BigDecimal.ZERO);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam updated = examService.assignTerm(publicId.toString(), publicId("term", 7).toString(),
				ExpectedVersion.any());

		assertEquals(7L, updated.getTermId());
	}

	@Test
	void assignTerm_pushingTheTermsWeightageOver100_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(termRepository.findByPublicIdAndTenantId(publicId("term", 7), 1L))
				.thenReturn(Optional.of(withId(Term.class, 7L)));
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, -1L))
				.thenReturn(new BigDecimal("40"));

		assertThrows(BusinessException.class,
				() -> examService.assignTerm(publicId.toString(), publicId("term", 7).toString(),
						ExpectedVersion.any()));

		verify(examRepository, never()).save(any());
	}

	private void stubValidReferences() {
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 10), 1L)).thenReturn(Optional.of(withId(Classroom.class, 10L)));
		when(subjectRepository.findByPublicIdAndTenantId(publicId("subject", 5), 1L)).thenReturn(Optional.of(withId(Subject.class, 5L)));
		lenient().when(termRepository.findByPublicIdAndTenantId(publicId("term", 7), 1L)).thenReturn(Optional.of(withId(Term.class, 7L)));
		when(examTypeDefinitionRepository.findByPublicIdAndTenantId(publicId("examType", 1), 1L)).thenReturn(Optional.of(withId(ExamTypeDefinition.class, 1L)));
	}

	@Test
	void schedule_withoutWeightage_defaultsToFullWeight() {
		stubValidReferences();
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, -1L))
				.thenReturn(BigDecimal.ZERO);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam exam = examService.schedule("Midterm", publicId("subject", 5).toString(),
				publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(100), publicId("term", 7).toString(), publicId("examType", 1).toString(), null);

		assertEquals(0, Exam.FULL_WEIGHTAGE.compareTo(exam.getWeightage()));
	}

	@Test
	void schedule_withinTheTermsRemainingWeightage_succeeds() {
		stubValidReferences();
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, -1L))
				.thenReturn(new BigDecimal("70"));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam exam = examService.schedule("Quiz", publicId("subject", 5).toString(),
				publicId("classroom", 10).toString(), LocalDateTime.now().plusDays(7),
				BigDecimal.valueOf(20), publicId("term", 7).toString(), publicId("examType", 1).toString(),
				new BigDecimal("30"));

		assertEquals(0, new BigDecimal("30").compareTo(exam.getWeightage()));
	}

	@Test
	void schedule_exceedingTheTermsWeightage_throwsBusinessException() {
		stubValidReferences();
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, -1L))
				.thenReturn(new BigDecimal("80"));

		assertThrows(BusinessException.class,
				() -> examService.schedule("Quiz", publicId("subject", 5).toString(),
						publicId("classroom", 10).toString(),
						LocalDateTime.now().plusDays(7), BigDecimal.valueOf(20), publicId("term", 7).toString(),
						publicId("examType", 1).toString(), new BigDecimal("30")));

		verify(examRepository, never()).save(any());
	}

	@Test
	void schedule_withoutATerm_skipsTheTermWeightageCheck() {
		stubValidReferences();
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		examService.schedule("Quiz", publicId("subject", 5).toString(), publicId("classroom", 10).toString(),
				LocalDateTime.now().plusDays(7), BigDecimal.valueOf(20), null, publicId("examType", 1).toString(),
				Exam.FULL_WEIGHTAGE);

		verify(examRepository, never()).sumWeightageOfOtherExams(any(), any(), any(), any(), any(), any());
	}

	@Test
	void reweight_updatesWeightageWithinTheTermCap() {
		UUID publicId = UUID.randomUUID();
		Exam exam = Exam.create("Midterm", 5L, 10L, LocalDateTime.now().plusDays(7), BigDecimal.valueOf(100), 7L, 1L,
				Exam.FULL_WEIGHTAGE);
		exam.setId(42L);
		exam.setPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.sumWeightageOfOtherExams(1L, 10L, 5L, 7L, ExamStatus.CANCELLED, 42L))
				.thenReturn(new BigDecimal("50"));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam reweighted = examService.reweight(publicId.toString(), new BigDecimal("50"), ExpectedVersion.any());

		assertEquals(0, new BigDecimal("50").compareTo(reweighted.getWeightage()));
	}

	@Test
	void publishResults_withRecordedGrades_publishesAndAnnouncesIt() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.setId(42L);
		exam.complete();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByExamIdAndTenantId(42L, 1L)).thenReturn(true);
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam published = examService.publishResults(publicId.toString(), "registrar");

		assertTrue(published.isResultsPublished());
		assertEquals("registrar", published.getResultsPublishedBy());
		verify(eventPublisher).publish(any(ExamResultsPublishedEvent.class));
	}

	@Test
	void publishResults_withoutAnyGrade_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.setId(42L);
		exam.complete();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByExamIdAndTenantId(42L, 1L)).thenReturn(false);

		assertThrows(BusinessException.class, () -> examService.publishResults(publicId.toString(), "registrar"));

		verify(eventPublisher, never()).publish(any());
	}

	@Test
	void withdrawResults_hidesPublishedResults() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.complete();
		exam.publishResults("registrar");
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam withdrawn = examService.withdrawResults(publicId.toString());

		assertFalse(withdrawn.isResultsPublished());
	}

	@Test
	void assignTerm_withNonExistentTerm_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(termRepository.findByPublicIdAndTenantId(publicId("term", 99), 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> examService.assignTerm(publicId.toString(), publicId("term", 99).toString(),
						ExpectedVersion.any()));
	}

	@Test
	void complete_scheduledExam_setsStatusCompleted() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam completed = examService.complete(publicId.toString());

		assertEquals(ExamStatus.COMPLETED, completed.getStatus());
	}

	@Test
	void complete_examOutsideCallersTeachingScope_throwsAccessDenied() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanWriteSubject(1L,
				exam.getClassroomId(), exam.getSubjectId());

		assertThrows(AccessDeniedException.class, () -> examService.complete(publicId.toString()));

		verify(examRepository, never()).save(any());
	}

	@Test
	void complete_alreadyCancelledExam_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.cancel();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));

		assertThrows(BusinessException.class, () -> examService.complete(publicId.toString()));
	}

	@Test
	void cancel_scheduledExam_setsStatusCancelled() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));
		when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

		Exam cancelled = examService.cancel(publicId.toString());

		assertEquals(ExamStatus.CANCELLED, cancelled.getStatus());
	}

	@Test
	void cancel_alreadyCompletedExam_throwsBusinessException() {
		UUID publicId = UUID.randomUUID();
		Exam exam = examWithPublicId(publicId);
		exam.complete();
		when(examRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(exam));

		assertThrows(BusinessException.class, () -> examService.cancel(publicId.toString()));
	}
}

package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.AcademicScope;
import com.altafjava.school.application.security.AcademicScopeResolver;
import com.altafjava.school.application.security.ExamResultVisibilityPolicy;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.security.TeachingAssignments;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.model.GradeCorrection;
import com.altafjava.school.domain.grade.repository.GradeCorrectionRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID EXAM_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private GradeCorrectionRepository gradeCorrectionRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private GradingScaleService gradingScaleService;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private AcademicScopeResolver academicScopeResolver;
	@Mock
	private AcademicAccessGuard academicAccessGuard;
	@Mock
	private ExamResultVisibilityPolicy examResultVisibilityPolicy;

	private GradeService gradeService;

	@BeforeEach
	void setUp() {
		gradeService = new GradeService(gradeRepository, gradeCorrectionRepository, studentRepository, examRepository,
				gradingScaleService, studentDataAccessGuard, academicScopeResolver, academicAccessGuard,
				examResultVisibilityPolicy);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void record_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.record(STUDENT_PUBLIC_ID.toString(), EXAM_PUBLIC_ID.toString(), BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_withNonExistentExam_throwsResourceNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(studentWithId(1L)));
		when(examRepository.findByPublicIdAndTenantId(EXAM_PUBLIC_ID, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> gradeService.record(STUDENT_PUBLIC_ID.toString(), EXAM_PUBLIC_ID.toString(), BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	@Test
	void record_duplicateForSameStudentExam_throwsIllegalArgument() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null,
				1L, Exam.FULL_WEIGHTAGE);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(1L)));
		exam.setId(2L);
		when(examRepository.findByPublicIdAndTenantId(EXAM_PUBLIC_ID, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> gradeService.record(STUDENT_PUBLIC_ID.toString(), EXAM_PUBLIC_ID.toString(),
						BigDecimal.valueOf(85), "teacher"));
	}

	@Test
	void record_withValidReferences_computesLetterGradeFromDefaultScale() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null,
				1L, Exam.FULL_WEIGHTAGE);
		List<GradingScaleThreshold> thresholds = List.of(
				GradingScaleThreshold.create(1L, "A", new BigDecimal("90"), new BigDecimal("4.0")),
				GradingScaleThreshold.create(1L, "F", BigDecimal.ZERO, BigDecimal.ZERO));
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(1L)));
		exam.setId(2L);
		when(examRepository.findByPublicIdAndTenantId(EXAM_PUBLIC_ID, 1L)).thenReturn(Optional.of(exam));
		when(gradeRepository.existsByStudentIdAndExamIdAndTenantId(1L, 2L, 1L)).thenReturn(false);
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(thresholds);
		when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

		Grade grade = assertDoesNotThrow(
				() -> gradeService.record(STUDENT_PUBLIC_ID.toString(), EXAM_PUBLIC_ID.toString(),
						BigDecimal.valueOf(92), "teacher"));

		assertEquals("A", grade.getGradeLetter());
		assertEquals(5L, grade.getSubjectId());
	}

	@Test
	void listGrades_withAllClassroomsScope_returnsAllTenantGrades() {
		when(academicScopeResolver.current(1L))
				.thenReturn(new AcademicScope(true, false, TeachingAssignments.NONE, Set.of()));
		PageRequest pageable = PageRequest.of(0, 20);
		when(gradeRepository.findAllByTenantId(1L, pageable)).thenReturn(Page.empty());

		gradeService.listGrades(pageable);

		verify(gradeRepository).findAllByTenantId(1L, pageable);
		verify(gradeRepository, never()).findVisible(any(), any(), any(), any());
	}

	@Test
	void listGrades_asSubjectTeacher_listsOnlyExamsOfSubjectsTheyTeach() {
		TeachingAssignments teaching = new TeachingAssignments(70L, Set.of(), Map.of(10L, Set.of(5L)));
		when(academicScopeResolver.current(1L)).thenReturn(new AcademicScope(false, false, teaching, Set.of()));
		Exam taught = examWithId(50L, 5L, 10L);
		Exam otherSubject = examWithId(51L, 6L, 10L);
		when(examRepository.findAllByClassroomIdInAndTenantId(Set.of(10L), 1L))
				.thenReturn(List.of(taught, otherSubject));
		PageRequest pageable = PageRequest.of(0, 20);
		when(gradeRepository.findVisible(1L, List.of(50L), Set.of(), pageable)).thenReturn(Page.empty());

		gradeService.listGrades(pageable);

		verify(gradeRepository).findVisible(1L, List.of(50L), Set.of(), pageable);
		verify(gradeRepository, never()).findAllByTenantId(any(), any());
	}

	@Test
	void listGrades_asParent_listsOnlyOwnStudentsGrades() {
		when(academicScopeResolver.current(1L))
				.thenReturn(new AcademicScope(false, false, TeachingAssignments.NONE, Set.of(30L)));
		when(examRepository.findAllByClassroomIdInAndTenantId(Set.of(), 1L)).thenReturn(List.of());
		PageRequest pageable = PageRequest.of(0, 20);
		when(gradeRepository.findVisible(1L, List.of(), Set.of(30L), pageable)).thenReturn(Page.empty());

		gradeService.listGrades(pageable);

		verify(gradeRepository).findVisible(1L, List.of(), Set.of(30L), pageable);
	}

	@Test
	void record_outsideCallersTeachingScope_throwsAccessDenied() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null, 1L, Exam.FULL_WEIGHTAGE);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(1L)));
		exam.setId(2L);
		when(examRepository.findByPublicIdAndTenantId(EXAM_PUBLIC_ID, 1L)).thenReturn(Optional.of(exam));
		doThrow(new AccessDeniedException("not scoped")).when(academicAccessGuard).assertCanWriteSubject(1L, 10L, 5L);

		assertThrows(AccessDeniedException.class,
				() -> gradeService.record(STUDENT_PUBLIC_ID.toString(), EXAM_PUBLIC_ID.toString(),
						BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	private Exam examWithId(Long id, Long subjectId, Long classroomId) {
		Exam exam = Exam.create("Midterm", subjectId, classroomId, null, BigDecimal.valueOf(100), null, 1L,
				Exam.FULL_WEIGHTAGE);
		exam.setId(id);
		return exam;
	}

	@Test
	void getStudentGrades_delegatesToAccessGuard() {
		doNothing().when(studentDataAccessGuard).assertCanView(any(), any());
		when(studentRepository.findByPublicIdAndTenantId(any(), any()))
				.thenReturn(Optional.of(com.altafjava.school.domain.student.model.Student.create(
						"STU-1", "Alice", "Smith", "alice@school.test", null)));

		assertDoesNotThrow(() -> gradeService.getStudentGrades(
				"11111111-1111-1111-1111-111111111111", org.springframework.data.domain.PageRequest.of(0, 20)));

		verify(studentDataAccessGuard).assertCanView(1L, "11111111-1111-1111-1111-111111111111");
	}

	@Test
	void correct_recordsCorrectionWithPreviousValuesThenUpdatesGrade() {
		Grade grade = Grade.create(1L, 5L, 2L, BigDecimal.valueOf(60), "D", "teacher");
		grade.setId(100L);
		grade.setPublicId(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null,
				1L, Exam.FULL_WEIGHTAGE);
		List<GradingScaleThreshold> thresholds = List.of(
				GradingScaleThreshold.create(1L, "A", new BigDecimal("90"), new BigDecimal("4.0")),
				GradingScaleThreshold.create(1L, "F", BigDecimal.ZERO, BigDecimal.ZERO));
		when(gradeRepository.findByPublicIdAndTenantId(grade.getPublicId(), 1L)).thenReturn(Optional.of(grade));
		when(examRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(exam));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(thresholds);
		when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

		Grade corrected = gradeService.correct("11111111-1111-1111-1111-111111111111", BigDecimal.valueOf(95),
				ExpectedVersion.any());

		assertEquals("A", corrected.getGradeLetter());
		assertEquals(BigDecimal.valueOf(95), corrected.getMarks());

		org.mockito.ArgumentCaptor<GradeCorrection> captor = org.mockito.ArgumentCaptor.forClass(GradeCorrection.class);
		verify(gradeCorrectionRepository).save(captor.capture());
		GradeCorrection correction = captor.getValue();
		assertEquals(BigDecimal.valueOf(60), correction.getOldMarks());
		assertEquals("D", correction.getOldGradeLetter());
		assertEquals(BigDecimal.valueOf(95), correction.getNewMarks());
		assertEquals("A", correction.getNewGradeLetter());
	}

	private Exam publishedExam() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null, 1L, Exam.FULL_WEIGHTAGE);
		exam.complete();
		exam.publishResults("registrar");
		return exam;
	}

	private Grade gradeOf(UUID publicId) {
		Grade grade = Grade.create(1L, 5L, 2L, BigDecimal.valueOf(60), "D", "teacher");
		grade.setPublicId(publicId);
		return grade;
	}

	@Test
	void findByPublicId_asStaff_returnsGradeWithoutOwnershipOrPublicationChecks() {
		UUID publicId = UUID.randomUUID();
		when(gradeRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(gradeOf(publicId)));
		when(examResultVisibilityPolicy.canSeeUnpublishedResults()).thenReturn(true);

		assertEquals(publicId, gradeService.findByPublicId(publicId.toString()).getPublicId());

		verify(studentDataAccessGuard, never()).assertCanView(any(), any());
	}

	@Test
	void findByPublicId_asStudentOfPublishedExam_returnsGrade() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setPublicId(UUID.randomUUID());
		when(gradeRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(gradeOf(publicId)));
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(examRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(publishedExam()));

		assertEquals(publicId, gradeService.findByPublicId(publicId.toString()).getPublicId());

		verify(studentDataAccessGuard).assertCanView(1L, student.getPublicId().toString());
	}

	@Test
	void findByPublicId_asStudentOfUnpublishedExam_throwsNotFound() {
		UUID publicId = UUID.randomUUID();
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setPublicId(UUID.randomUUID());
		Exam unpublished = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null, 1L,
				Exam.FULL_WEIGHTAGE);
		when(gradeRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(gradeOf(publicId)));
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(examRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(unpublished));

		assertThrows(ResourceNotFoundException.class, () -> gradeService.findByPublicId(publicId.toString()));
	}

	@Test
	void getStudentGrades_asStaff_listsEveryGrade() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(
				Student.create("STU-1", "Alice", "Smith", "alice@school.test", null)));
		when(examResultVisibilityPolicy.canSeeUnpublishedResults()).thenReturn(true);

		gradeService.getStudentGrades("11111111-1111-1111-1111-111111111111",
				org.springframework.data.domain.PageRequest.of(0, 20));

		verify(gradeRepository).findByStudentIdAndTenantId(any(), any(), any());
		verify(gradeRepository, never()).findPublishedByStudentId(any(), any(), any());
	}

	@Test
	void getStudentGrades_asStudentOrGuardian_listsPublishedGradesOnly() {
		when(studentRepository.findByPublicIdAndTenantId(any(), any())).thenReturn(Optional.of(
				Student.create("STU-1", "Alice", "Smith", "alice@school.test", null)));
		when(examResultVisibilityPolicy.canSeeUnpublishedResults()).thenReturn(false);

		gradeService.getStudentGrades("11111111-1111-1111-1111-111111111111",
				org.springframework.data.domain.PageRequest.of(0, 20));

		verify(gradeRepository).findPublishedByStudentId(any(), any(), any());
		verify(gradeRepository, never()).findByStudentIdAndTenantId(any(), any(), any());
	}

	@Test
	void record_forCancelledExam_throwsBusinessException() {
		Exam exam = Exam.create("Midterm", 5L, 10L, null, BigDecimal.valueOf(100), null, 1L, Exam.FULL_WEIGHTAGE);
		exam.cancel();
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(1L)));
		exam.setId(2L);
		when(examRepository.findByPublicIdAndTenantId(EXAM_PUBLIC_ID, 1L)).thenReturn(Optional.of(exam));

		assertThrows(BusinessException.class, () -> gradeService.record(STUDENT_PUBLIC_ID.toString(),
				EXAM_PUBLIC_ID.toString(), BigDecimal.valueOf(85), "teacher"));

		verify(gradeRepository, never()).save(any());
	}

	private Student studentWithId(Long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}
}

package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.security.ExamResultVisibilityPolicy;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class StudentGpaServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private TermRepository termRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private GradingScaleService gradingScaleService;
	@Mock
	private ExamResultVisibilityPolicy examResultVisibilityPolicy;

	private StudentGpaService studentGpaService;

	@BeforeEach
	void setUp() {
		studentGpaService = new StudentGpaService(gradeRepository, examRepository, termRepository,
				academicYearRepository, studentRepository, studentDataAccessGuard, gradingScaleService,
				examResultVisibilityPolicy);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(3L);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L)).thenReturn(Optional.of(student));
		doNothing().when(studentDataAccessGuard).assertCanView(1L, STUDENT_PUBLIC_ID.toString());
		lenient().when(examResultVisibilityPolicy.canSeeUnpublishedResults()).thenReturn(true);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Grade gradeWithExam(long examId, String letter) {
		Grade grade = Grade.create(3L, 5L, examId, BigDecimal.valueOf(90), letter, "teacher");
		return grade;
	}

	private Exam examScheduledAt(long id, long classroomId, LocalDateTime scheduledAt) {
		Exam exam = Exam.create("Midterm", 5L, classroomId, scheduledAt, BigDecimal.valueOf(100), null,
				1L, Exam.FULL_WEIGHTAGE);
		exam.setId(id);
		return exam;
	}

	@Test
	void calculateCumulativeGpa_withGrades_returnsAverage() {
		Grade gradeA = gradeWithExam(1L, "A");
		Grade gradeB = gradeWithExam(2L, "B");
		Exam examA = examScheduledAt(1L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0));
		Exam examB = examScheduledAt(2L, 10L, LocalDateTime.of(2026, 5, 2, 9, 0));
		when(gradeRepository.findByStudentId(1L, 3L)).thenReturn(List.of(gradeA, gradeB));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L)).thenReturn(List.of(examA, examB));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0")),
				GradingScaleThreshold.create(99L, "B", new BigDecimal("80"), new BigDecimal("3.0"))));

		GpaResult result = studentGpaService.calculateCumulativeGpa(STUDENT_PUBLIC_ID.toString());

		assertEquals(2, result.gradeCount());
		assertEquals(0, new BigDecimal("3.50").compareTo(result.gpa()));
	}

	@Test
	void calculateCumulativeGpa_noGrades_returnsNullGpaWithZeroCount() {
		when(gradeRepository.findByStudentId(1L, 3L)).thenReturn(List.of());

		GpaResult result = studentGpaService.calculateCumulativeGpa(STUDENT_PUBLIC_ID.toString());

		assertEquals(0, result.gradeCount());
		assertNull(result.gpa());
	}

	@Test
	void calculateCumulativeGpa_unresolvableLetterOnCurrentScale_excludesThatGrade() {
		Grade gradeA = gradeWithExam(1L, "A");
		Grade gradeStale = gradeWithExam(2L, "OLD");
		Exam examA = examScheduledAt(1L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0));
		Exam examStale = examScheduledAt(2L, 10L, LocalDateTime.of(2026, 5, 2, 9, 0));
		when(gradeRepository.findByStudentId(1L, 3L)).thenReturn(List.of(gradeA, gradeStale));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L)).thenReturn(List.of(examA, examStale));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0"))));

		GpaResult result = studentGpaService.calculateCumulativeGpa(STUDENT_PUBLIC_ID.toString());

		assertEquals(1, result.gradeCount());
		assertEquals(0, new BigDecimal("4.00").compareTo(result.gpa()));
	}

	@Test
	void calculateTermGpa_onlyIncludesGradesWithinTermRange() {
		UUID termPublicId = UUID.randomUUID();
		Term term = Term.create("Term 1", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), 1L);
		when(termRepository.findByPublicIdAndTenantId(termPublicId, 1L)).thenReturn(Optional.of(term));
		Grade inTerm = gradeWithExam(1L, "A");
		Grade outOfTerm = gradeWithExam(2L, "B");
		Exam examInTerm = examScheduledAt(1L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0));
		Exam examOutOfTerm = examScheduledAt(2L, 10L, LocalDateTime.of(2026, 8, 1, 9, 0));
		when(gradeRepository.findByStudentId(1L, 3L)).thenReturn(List.of(inTerm, outOfTerm));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L))
				.thenReturn(List.of(examInTerm, examOutOfTerm));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0"))));

		GpaResult result = studentGpaService.calculateTermGpa(STUDENT_PUBLIC_ID.toString(), termPublicId.toString());

		assertEquals(1, result.gradeCount());
	}

	@Test
	void calculateAcademicYearGpa_onlyIncludesGradesWithinYearRange() {
		UUID academicYearPublicId = UUID.randomUUID();
		AcademicYear academicYear = AcademicYear.create("2026-27", LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31), true);
		when(academicYearRepository.findByPublicIdAndTenantId(academicYearPublicId, 1L))
				.thenReturn(Optional.of(academicYear));
		Grade inYear = gradeWithExam(1L, "A");
		Exam examInYear = examScheduledAt(1L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0));
		when(gradeRepository.findByStudentId(1L, 3L)).thenReturn(List.of(inYear));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(examInYear));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0"))));

		GpaResult result = studentGpaService.calculateAcademicYearGpa(STUDENT_PUBLIC_ID.toString(),
				academicYearPublicId.toString());

		assertEquals(1, result.gradeCount());
		assertEquals(0, new BigDecimal("4.00").compareTo(result.gpa()));
	}

	@Test
	void calculateCumulativeGpa_weightsGradePointsByExamWeightage() {
		Exam heavy = Exam.create("Final", 5L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0), BigDecimal.valueOf(100), null,
				1L, new BigDecimal("80"));
		heavy.setId(1L);
		Exam light = Exam.create("Quiz", 5L, 10L, LocalDateTime.of(2026, 5, 2, 9, 0), BigDecimal.valueOf(100), null,
				1L, new BigDecimal("20"));
		light.setId(2L);
		when(gradeRepository.findByStudentId(1L, 3L))
				.thenReturn(List.of(gradeWithExam(1L, "B"), gradeWithExam(2L, "A")));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L)).thenReturn(List.of(heavy, light));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0")),
				GradingScaleThreshold.create(99L, "B", new BigDecimal("80"), new BigDecimal("3.0"))));

		GpaResult result = studentGpaService.calculateCumulativeGpa(STUDENT_PUBLIC_ID.toString());

		// (3.0 * 80 + 4.0 * 20) / 100
		assertEquals(0, new BigDecimal("3.20").compareTo(result.gpa()));
	}

	@Test
	void calculateCumulativeGpa_withoutUnpublishedAccess_excludesUnpublishedExams() {
		when(examResultVisibilityPolicy.canSeeUnpublishedResults()).thenReturn(false);
		Exam published = examScheduledAt(1L, 10L, LocalDateTime.of(2026, 5, 1, 9, 0));
		published.complete();
		published.publishResults("registrar");
		Exam unpublished = examScheduledAt(2L, 10L, LocalDateTime.of(2026, 5, 2, 9, 0));
		when(gradeRepository.findByStudentId(1L, 3L))
				.thenReturn(List.of(gradeWithExam(1L, "A"), gradeWithExam(2L, "B")));
		when(examRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L)).thenReturn(List.of(published, unpublished));
		when(gradingScaleService.resolveEffectiveThresholds(10L)).thenReturn(List.of(
				GradingScaleThreshold.create(99L, "A", new BigDecimal("90"), new BigDecimal("4.0")),
				GradingScaleThreshold.create(99L, "B", new BigDecimal("80"), new BigDecimal("3.0"))));

		GpaResult result = studentGpaService.calculateCumulativeGpa(STUDENT_PUBLIC_ID.toString());

		assertEquals(1, result.gradeCount());
		assertEquals(0, new BigDecimal("4.00").compareTo(result.gpa()));
	}
}

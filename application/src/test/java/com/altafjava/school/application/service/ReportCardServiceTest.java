package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import com.altafjava.platform.application.branding.TenantBrandingService;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.reportcard.ReportCardLine;
import com.altafjava.school.application.reportcard.ReportCardPdfGenerator;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.model.Grade;
import com.altafjava.school.domain.grade.repository.GradeRepository;
import com.altafjava.school.domain.holiday.repository.HolidayRepository;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.reportcard.repository.ReportCardRepository;
import com.altafjava.school.domain.reportcard.repository.ReportCardTemplateRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.term.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

	@Mock
	private ReportCardRepository reportCardRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private TermRepository termRepository;
	@Mock
	private GradeRepository gradeRepository;
	@Mock
	private ExamRepository examRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private StorageService storageService;
	@Mock
	private ReportCardPdfGenerator pdfGenerator;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private EventPublisher eventPublisher;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private TenantBrandingService tenantBrandingService;
	@Mock
	private TenantFormattingService tenantFormattingService;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private HolidayRepository holidayRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private ReportCardTemplateRepository reportCardTemplateRepository;
	@Mock
	private CustomFieldValueService customFieldValueService;

	private ReportCardService reportCardService;

	@BeforeEach
	void setUp() {
		// TransactionTemplate.execute() calls transactionManager.getTransaction(...) then commit(...)
		// around the callback — stub just enough of the real contract for the callback to run.
		lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
		Tenant tenant = Tenant.builder().name("Test School").build();
		lenient().when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
		lenient().when(tenantBrandingService.getLogoBytes(1L)).thenReturn(Optional.empty());
		lenient().when(tenantFormattingService.resolveLocale(1L)).thenReturn(java.util.Locale.US);
		reportCardService = new ReportCardService(reportCardRepository, studentRepository, termRepository,
				gradeRepository, examRepository, subjectRepository, storageService, pdfGenerator,
				studentDataAccessGuard, eventPublisher, transactionManager, tenantRepository, tenantBrandingService,
				tenantFormattingService, attendanceRepository, holidayRepository, studentClassroomLinkRepository,
				classroomRepository, reportCardTemplateRepository, customFieldValueService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Term termWithId(long id, LocalDate start, LocalDate end) {
		Term term = Term.create("Term 1", start, end, 1L);
		term.setId(id);
		return term;
	}

	private Grade gradeWithId(long id, long examId, BigDecimal marks) {
		Grade grade = Grade.create(1L, 5L, examId, marks, "A", "teacher");
		grade.setId(id);
		return grade;
	}

	private Exam examAt(long id, LocalDateTime scheduledAt) {
		Exam exam = Exam.create("Midterm", 5L, 2L, scheduledAt, BigDecimal.valueOf(100), null,
				1L);
		exam.setId(id);
		return exam;
	}

	@Test
	@SuppressWarnings("unchecked")
	void generate_withGradeInsideTermRange_includesItAndPersistsReportCard() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Grade grade = gradeWithId(100L, 50L, BigDecimal.valueOf(85));
		Exam exam = examAt(50L, LocalDateTime.of(2026, 2, 1, 9, 0));
		Subject subject = Subject.create("MATH", "Mathematics", null);
		subject.setId(5L);

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(grade));
		when(examRepository.findAllByIdInAndTenantId(List.of(50L), 1L)).thenReturn(List.of(exam));
		when(subjectRepository.findAllByIdInAndTenantId(List.of(5L), 1L)).thenReturn(List.of(subject));
		when(pdfGenerator.generate(eq(student), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		ReportCard result = reportCardService.generate(1L, 10L, null, null);

		ArgumentCaptor<List<ReportCardLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
		verify(pdfGenerator).generate(eq(student), eq(term), linesCaptor.capture(), anyString(), any(), any(), any());
		assertEquals(1, linesCaptor.getValue().size());
		assertEquals("Mathematics", linesCaptor.getValue().get(0).subjectName());
		assertEquals(10L, result.getTermId());
		assertEquals(1L, result.getStudentId());
		verify(storageService).uploadFile(anyString(), any(byte[].class), eq("application/pdf"));
		verify(eventPublisher).publish(any());
		// Batched — exactly one IN-query per lookup type, never one per Grade row.
		verify(examRepository, times(1)).findAllByIdInAndTenantId(any(), any());
		verify(subjectRepository, times(1)).findAllByIdInAndTenantId(any(), any());
		verify(examRepository, never()).findByIdAndTenantId(any(), any());
		verify(subjectRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void generate_withGradeOutsideTermRange_excludesIt() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Grade grade = gradeWithId(100L, 50L, BigDecimal.valueOf(85));
		Exam examOutsideRange = examAt(50L, LocalDateTime.of(2026, 6, 1, 9, 0));

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(grade));
		when(examRepository.findAllByIdInAndTenantId(List.of(50L), 1L)).thenReturn(List.of(examOutsideRange));
		when(pdfGenerator.generate(eq(student), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		reportCardService.generate(1L, 10L, null, null);

		ArgumentCaptor<List<ReportCardLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
		verify(pdfGenerator).generate(eq(student), eq(term), linesCaptor.capture(), anyString(), any(), any(), any());
		assertTrue(linesCaptor.getValue().isEmpty());
		// The out-of-range exam means no grade survives to the subject-batching step at all.
		verify(subjectRepository, never()).findByIdAndTenantId(any(), any());
		verify(subjectRepository, never()).findAllByIdInAndTenantId(any(), any());
	}

	@Test
	void generate_whenReportCardAlreadyExistsForTerm_softDeletesPreviousOne() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		ReportCard existing = ReportCard.create(1L, 10L, "old-key.pdf");

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of());
		when(pdfGenerator.generate(eq(student), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.of(existing));
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		reportCardService.generate(1L, 10L, null, null);

		assertTrue(existing.isDeleted());
		verify(reportCardRepository, times(2)).save(any(ReportCard.class));
		verify(storageService).deleteFile("old-key.pdf");
	}

	@Test
	void generate_withNonExistentStudent_throwsResourceNotFound() {
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.empty());

		org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
				() -> reportCardService.generate(1L, 10L, null, null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void generate_withMultipleGradesAcrossDifferentExamsAndSubjects_batchesLookupsInsteadOfPerRowQueries() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Grade gradeA = Grade.create(1L, 5L, 50L, BigDecimal.valueOf(85), "A", "teacher");
		gradeA.setId(100L);
		Grade gradeB = Grade.create(1L, 6L, 51L, BigDecimal.valueOf(90), "A", "teacher");
		gradeB.setId(101L);
		Grade gradeC = Grade.create(1L, 5L, 52L, BigDecimal.valueOf(70), "B", "teacher");
		gradeC.setId(102L);
		Exam examA = examAt(50L, LocalDateTime.of(2026, 2, 1, 9, 0));
		Exam examB = examAt(51L, LocalDateTime.of(2026, 2, 5, 9, 0));
		Exam examC = examAt(52L, LocalDateTime.of(2026, 2, 10, 9, 0));
		Subject subjectMath = Subject.create("MATH", "Mathematics", null);
		subjectMath.setId(5L);
		Subject subjectSci = Subject.create("SCI", "Science", null);
		subjectSci.setId(6L);

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(gradeA, gradeB, gradeC));
		when(examRepository.findAllByIdInAndTenantId(List.of(50L, 51L, 52L), 1L))
				.thenReturn(List.of(examA, examB, examC));
		when(subjectRepository.findAllByIdInAndTenantId(List.of(5L, 6L), 1L))
				.thenReturn(List.of(subjectMath, subjectSci));
		when(pdfGenerator.generate(eq(student), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));

		reportCardService.generate(1L, 10L, null, null);

		ArgumentCaptor<List<ReportCardLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
		verify(pdfGenerator).generate(eq(student), eq(term), linesCaptor.capture(), anyString(), any(), any(), any());
		assertEquals(3, linesCaptor.getValue().size());
		// Exactly one batched IN-query for exams and one for subjects, no matter how many grades.
		verify(examRepository, times(1)).findAllByIdInAndTenantId(any(), any());
		verify(subjectRepository, times(1)).findAllByIdInAndTenantId(any(), any());
		verify(examRepository, never()).findByIdAndTenantId(any(), any());
		verify(subjectRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	void generate_whenDbWriteFailsAfterSuccessfulUpload_cleansUpOrphanedUploadAndRethrows() {
		Student student = studentWithId(1L);
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of());
		when(pdfGenerator.generate(eq(student), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(1L, 10L, 1L)).thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenThrow(new RuntimeException("db unavailable"));

		assertThrows(RuntimeException.class, () -> reportCardService.generate(1L, 10L, null, null));

		ArgumentCaptor<String> uploadedKeyCaptor = ArgumentCaptor.forClass(String.class);
		verify(storageService).uploadFile(uploadedKeyCaptor.capture(), any(byte[].class), eq("application/pdf"));
		verify(storageService).deleteFile(uploadedKeyCaptor.getValue());
	}

	@Test
	void generate_twoClassmatesWithSharedRankCache_computesClassroomRankingOnlyOnce() {
		Term term = termWithId(10L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
		Student student1 = studentWithId(1L);
		Student student2 = studentWithId(2L);
		Classroom classroom = Classroom.create("5A", "5", "A", 1L, "2026", null);
		classroom.setId(20L);
		StudentClassroomLink link1 = StudentClassroomLink.create(1L, 20L, 1L, LocalDate.of(2025, 6, 1));
		StudentClassroomLink link2 = StudentClassroomLink.create(2L, 20L, 1L, LocalDate.of(2025, 6, 1));
		Exam exam = examAt(50L, LocalDateTime.of(2026, 2, 1, 9, 0));
		Grade gradeStudent1 = Grade.create(1L, 5L, 50L, BigDecimal.valueOf(90), "A", "teacher");
		gradeStudent1.setId(100L);
		Grade gradeStudent2 = Grade.create(2L, 5L, 50L, BigDecimal.valueOf(70), "B", "teacher");
		gradeStudent2.setId(101L);

		when(studentRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(student1));
		when(studentRepository.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(student2));
		when(termRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(term));
		when(gradeRepository.findByStudentId(1L, 1L)).thenReturn(List.of(gradeStudent1));
		when(gradeRepository.findByStudentId(1L, 2L)).thenReturn(List.of(gradeStudent2));
		when(examRepository.findAllByIdInAndTenantId(List.of(50L), 1L)).thenReturn(List.of(exam));
		when(pdfGenerator.generate(any(), eq(term), any(), anyString(), any(), any(), any()))
				.thenReturn("pdf-bytes".getBytes());
		when(reportCardRepository.findByStudentIdAndTermIdAndTenantId(any(), eq(10L), eq(1L)))
				.thenReturn(Optional.empty());
		when(reportCardRepository.save(any(ReportCard.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentClassroomLinkRepository.findByStudentId(1L, 1L)).thenReturn(List.of(link1));
		when(studentClassroomLinkRepository.findByStudentId(1L, 2L)).thenReturn(List.of(link2));
		when(classroomRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(classroom));
		when(studentClassroomLinkRepository.findAllByClassroomId(1L, 20L)).thenReturn(List.of(link1, link2));
		when(gradeRepository.findByStudentIdInAndTenantId(List.of(1L, 2L), 1L))
				.thenReturn(List.of(gradeStudent1, gradeStudent2));

		java.util.Map<Long, Integer> classroomRankCache = new java.util.HashMap<>();
		reportCardService.generate(1L, 10L, null, null, classroomRankCache);
		reportCardService.generate(2L, 10L, null, null, classroomRankCache);

		// The expensive per-classroom lookups run once — the second student's rank comes from the
		// shared cache the first student's generation populated, not a repeat query.
		verify(studentClassroomLinkRepository, times(1)).findAllByClassroomId(1L, 20L);
		verify(gradeRepository, times(1)).findByStudentIdInAndTenantId(List.of(1L, 2L), 1L);
		assertEquals(1, classroomRankCache.get(1L));
		assertEquals(2, classroomRankCache.get(2L));
	}
}

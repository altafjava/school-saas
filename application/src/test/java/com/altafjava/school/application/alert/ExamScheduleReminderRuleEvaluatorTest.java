package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@ExtendWith(MockitoExtension.class)
class ExamScheduleReminderRuleEvaluatorTest {

	@Mock
	private ExamRepository examRepository;
	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;
	@Mock
	private TenantFormattingService tenantFormattingService;

	private ExamScheduleReminderRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new ExamScheduleReminderRuleEvaluator(examRepository, subjectRepository, attendanceRepository,
				studentRepository, recipientResolver, tenantFormattingService);
	}

	private AlertRule rule() {
		AlertRule rule = AlertRule.create(ExamScheduleReminderRuleEvaluator.RULE_TYPE, "Exam reminder", true, null,
				NotificationType.EXAM_SCHEDULED, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Exam examWithId(long id, long classroomId, long subjectId) {
		Exam exam = Exam.create("Midterm", subjectId, classroomId, LocalDateTime.now().plusDays(1),
				BigDecimal.valueOf(100), null, 1L, Exam.FULL_WEIGHTAGE);
		exam.setId(id);
		return exam;
	}

	private Subject subjectWithId(long id, String name) {
		Subject subject = Subject.create("SUB-" + id, name, null);
		subject.setId(id);
		return subject;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void evaluate_studentInExamClassroom_returnsTrigger() {
		newEvaluator();
		Exam exam = examWithId(100L, 10L, 5L);
		Subject subject = subjectWithId(5L, "Math");
		Student student = studentWithId(1L);

		when(examRepository.findUpcoming(eq(1L), any(), any())).thenReturn(List.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(subject));
		when(attendanceRepository.findDistinctStudentIdsByClassroomId(1L, 10L)).thenReturn(List.of(1L));
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(77L));
		when(tenantFormattingService.localizedFormatter(eq(1L), any()))
				.thenReturn(DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm", Locale.US));

		List<AlertTrigger> triggers = evaluator.evaluate(rule());

		assertEquals(1, triggers.size());
		AlertTrigger trigger = triggers.get(0);
		assertEquals(77L, trigger.recipientUserId());
		assertEquals("Alice Smith", trigger.templateVariables().get("studentName"));
		assertEquals("Midterm", trigger.templateVariables().get("examTitle"));
		assertEquals("Math", trigger.templateVariables().get("subjectName"));
		verify(studentRepository, never()).findByIdAndTenantId(any(), any());
	}

	@Test
	void evaluate_multipleStudentsInClassroom_bulkFetchesRatherThanPerStudentLookup() {
		newEvaluator();
		Exam exam = examWithId(100L, 10L, 5L);
		Subject subject = subjectWithId(5L, "Math");
		Student student1 = studentWithId(1L);
		Student student2 = studentWithId(2L);

		when(examRepository.findUpcoming(eq(1L), any(), any())).thenReturn(List.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(subject));
		when(attendanceRepository.findDistinctStudentIdsByClassroomId(1L, 10L)).thenReturn(List.of(1L, 2L));
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L, 2L), 1L))
				.thenReturn(List.of(student1, student2));
		when(recipientResolver.resolve(eq(1L), any(Student.class))).thenReturn(Optional.of(77L));
		when(tenantFormattingService.localizedFormatter(eq(1L), any()))
				.thenReturn(DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm", Locale.US));

		List<AlertTrigger> triggers = evaluator.evaluate(rule());

		assertEquals(2, triggers.size());
		verify(studentRepository, never()).findByIdAndTenantId(any(), any());
		verify(studentRepository, org.mockito.Mockito.times(1)).findAllByIdInAndTenantId(any(), any());
	}

	@Test
	void evaluate_examWithMissingSubject_skipsWithoutError() {
		newEvaluator();
		Exam exam = examWithId(100L, 10L, 5L);
		when(examRepository.findUpcoming(eq(1L), any(), any())).thenReturn(List.of(exam));
		when(subjectRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.empty());

		assertTrue(evaluator.evaluate(rule()).isEmpty());
		verify(attendanceRepository, never()).findDistinctStudentIdsByClassroomId(any(), any());
	}

	@Test
	void evaluate_noUpcomingExams_returnsNoTriggers() {
		newEvaluator();
		when(examRepository.findUpcoming(eq(1L), any(), any())).thenReturn(List.of());

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}
}

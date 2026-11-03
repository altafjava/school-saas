package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.model.AttendanceTally;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LowAttendanceRuleEvaluatorTest {

	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;

	private LowAttendanceRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new LowAttendanceRuleEvaluator(studentClassroomLinkRepository, attendanceRepository,
				studentRepository, recipientResolver, TestSchoolClocks.systemDefault());
	}

	private AlertRule ruleWithThreshold(Integer thresholdPercent) {
		AlertRule rule = AlertRule.create(LowAttendanceRuleEvaluator.RULE_TYPE, "Low attendance", true,
				thresholdPercent == null ? null : BigDecimal.valueOf(thresholdPercent),
				SchoolNotificationTypes.LOW_ATTENDANCE_ALERT, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void evaluate_studentBelowThreshold_returnsTrigger() {
		newEvaluator();
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.tallyByStudent(eq(1L), eq(List.of(1L)), any(LocalDate.class),
				any(LocalDate.class), eq(true), any()))
				.thenReturn(List.of(new AttendanceTally(1L, AttendanceStatus.PRESENT, 10L),
						new AttendanceTally(1L, AttendanceStatus.ABSENT, 10L)));
		Student student = studentWithId(1L);
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(java.util.Optional.of(77L));

		List<AlertTrigger> triggers = evaluator.evaluate(ruleWithThreshold(75));

		assertEquals(1, triggers.size());
		AlertTrigger trigger = triggers.get(0);
		assertEquals(77L, trigger.recipientUserId());
		assertEquals("Alice Smith", trigger.templateVariables().get("studentName"));
		assertEquals("50.00", trigger.templateVariables().get("percentage"));
		assertEquals("30", trigger.templateVariables().get("windowDays"));
	}

	@Test
	void evaluate_studentAtOrAboveThreshold_returnsNoTriggers() {
		newEvaluator();
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.tallyByStudent(eq(1L), eq(List.of(1L)), any(LocalDate.class),
				any(LocalDate.class), eq(true), any()))
				.thenReturn(List.of(new AttendanceTally(1L, AttendanceStatus.PRESENT, 16L),
						new AttendanceTally(1L, AttendanceStatus.ABSENT, 4L)));

		assertTrue(evaluator.evaluate(ruleWithThreshold(75)).isEmpty());
	}

	@Test
	void evaluate_studentWithNoMarkedAttendance_skipsWithoutError() {
		newEvaluator();
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.tallyByStudent(eq(1L), eq(List.of(1L)), any(LocalDate.class),
				any(LocalDate.class), eq(true), any())).thenReturn(List.of());

		assertTrue(evaluator.evaluate(ruleWithThreshold(75)).isEmpty());
	}

	@Test
	void evaluate_nullThreshold_usesDefaultSeventyFivePercent() {
		newEvaluator();
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of(1L));
		when(attendanceRepository.tallyByStudent(eq(1L), eq(List.of(1L)), any(LocalDate.class),
				any(LocalDate.class), eq(true), any()))
				.thenReturn(List.of(new AttendanceTally(1L, AttendanceStatus.PRESENT, 10L),
						new AttendanceTally(1L, AttendanceStatus.ABSENT, 10L)));
		Student student = studentWithId(1L);
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(java.util.Optional.of(77L));

		assertEquals(1, evaluator.evaluate(ruleWithThreshold(null)).size());
	}

	@Test
	void evaluate_noRosterStudents_returnsNoTriggers() {
		newEvaluator();
		when(studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(1L)).thenReturn(List.of());

		assertTrue(evaluator.evaluate(ruleWithThreshold(75)).isEmpty());
	}
}

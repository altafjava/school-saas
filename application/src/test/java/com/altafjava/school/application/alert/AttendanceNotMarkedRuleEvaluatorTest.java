package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationTypes;
import com.altafjava.school.application.time.TestSchoolClocks;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceNotMarkedRuleEvaluatorTest {

	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private TeacherRepository teacherRepository;

	private AttendanceNotMarkedRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new AttendanceNotMarkedRuleEvaluator(classroomRepository, attendanceRepository,
				teacherRepository, TestSchoolClocks.systemDefault());
	}

	private AlertRule rule() {
		AlertRule rule = AlertRule.create(AttendanceNotMarkedRuleEvaluator.RULE_TYPE, "Attendance reminder", true,
				null, NotificationTypes.ANNOUNCEMENT, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Classroom classroomWithTeacher(long id, Long teacherId) {
		Classroom classroom = Classroom.create("CLS-" + id, "Grade 5", "A", 1L, "2025-26", teacherId);
		classroom.setId(id);
		return classroom;
	}

	private Teacher teacherWithId(long id, Long userId) {
		Teacher teacher = Teacher.create("EMP-" + id, "Jane", "Doe", "jane@school.test", null);
		teacher.setId(id);
		teacher.setUserId(userId);
		return teacher;
	}

	@Test
	void evaluate_classroomWithoutTodaysAttendance_returnsTriggerForTeacher() {
		newEvaluator();
		Classroom classroom = classroomWithTeacher(10L, 20L);
		Teacher teacher = teacherWithId(20L, 99L);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.findDistinctClassroomIdsMarkedOnDate(1L, LocalDate.now())).thenReturn(List.of());
		when(teacherRepository.findAllByIdInAndTenantId(List.of(20L), 1L)).thenReturn(List.of(teacher));

		List<AlertTrigger> triggers = evaluator.evaluate(rule());

		assertEquals(1, triggers.size());
		assertEquals(99L, triggers.get(0).recipientUserId());
	}

	@Test
	void evaluate_classroomWithTodaysAttendanceAlreadyMarked_returnsNoTriggers() {
		newEvaluator();
		Classroom classroom = classroomWithTeacher(10L, 20L);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.findDistinctClassroomIdsMarkedOnDate(1L, LocalDate.now()))
				.thenReturn(List.of(10L));

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}

	@Test
	void evaluate_classroomWithNoTeacherAssigned_returnsNoTriggers() {
		newEvaluator();
		Classroom classroom = classroomWithTeacher(10L, null);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}

	@Test
	void evaluate_teacherWithNoLoginAccount_returnsNoTriggers() {
		newEvaluator();
		Classroom classroom = classroomWithTeacher(10L, 20L);
		Teacher teacher = teacherWithId(20L, null);
		when(classroomRepository.findAllByTenantId(1L)).thenReturn(List.of(classroom));
		when(attendanceRepository.findDistinctClassroomIdsMarkedOnDate(1L, LocalDate.now())).thenReturn(List.of());
		when(teacherRepository.findAllByIdInAndTenantId(List.of(20L), 1L)).thenReturn(List.of(teacher));

		assertTrue(evaluator.evaluate(rule()).isEmpty());
	}
}

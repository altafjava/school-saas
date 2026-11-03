package com.altafjava.school.application.alert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * {@code ruleType = "DAILY_ATTENDANCE_NOT_MARKED"}: reminds a classroom's class teacher — not a
 * student/guardian — when attendance hasn't been marked yet today (boolean condition, threshold
 * unused). Drives {@code DailyAttendanceReminderJob}.
 */
@Component
public class AttendanceNotMarkedRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "DAILY_ATTENDANCE_NOT_MARKED";

	private final ClassroomRepository classroomRepository;
	private final AttendanceRepository attendanceRepository;
	private final TeacherRepository teacherRepository;
	private final SchoolClock schoolClock;

	public AttendanceNotMarkedRuleEvaluator(ClassroomRepository classroomRepository,
			AttendanceRepository attendanceRepository, TeacherRepository teacherRepository, SchoolClock schoolClock) {
		this.classroomRepository = classroomRepository;
		this.attendanceRepository = attendanceRepository;
		this.teacherRepository = teacherRepository;
		this.schoolClock = schoolClock;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		LocalDate today = schoolClock.today(tenantId);

		List<Classroom> unmarkedClassroomsWithTeacher = unmarkedClassroomsWithTeacher(tenantId, today);
		if (unmarkedClassroomsWithTeacher.isEmpty()) {
			return List.of();
		}

		List<Long> teacherIds = unmarkedClassroomsWithTeacher.stream()
				.map(Classroom::getClassTeacherId)
				.distinct()
				.toList();
		Map<Long, Teacher> teachersById = teacherRepository.findAllByIdInAndTenantId(teacherIds, tenantId).stream()
				.collect(Collectors.toMap(Teacher::getId, Function.identity()));

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Classroom classroom : unmarkedClassroomsWithTeacher) {
			Teacher teacher = teachersById.get(classroom.getClassTeacherId());
			if (teacher == null || teacher.getUserId() == null) {
				continue;
			}
			triggers.add(new AlertTrigger(teacher.getUserId(), "Attendance Reminder",
					"Attendance has not yet been marked today for classroom " + classroom.getClassCode() + ".",
					Map.of(), NotificationPriority.NORMAL));
		}
		return triggers;
	}

	private List<Classroom> unmarkedClassroomsWithTeacher(Long tenantId, LocalDate today) {
		Set<Long> markedClassroomIds = new HashSet<>(
				attendanceRepository.findDistinctClassroomIdsMarkedOnDate(tenantId, today));
		return classroomRepository.findAllByTenantId(tenantId).stream()
				.filter(classroom -> classroom.getClassTeacherId() != null)
				.filter(classroom -> !markedClassroomIds.contains(classroom.getId()))
				.toList();
	}
}

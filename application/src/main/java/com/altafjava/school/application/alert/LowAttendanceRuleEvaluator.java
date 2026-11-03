package com.altafjava.school.application.alert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertRuleEvaluator;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.attendance.model.AttendancePercentage;
import com.altafjava.school.domain.attendance.model.AttendanceTally;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.attendance.service.AttendancePercentageCalculator;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * {@code ruleType = "LOW_ATTENDANCE"}: flags active-roster students whose trailing 30-day
 * attendance falls below the rule's threshold (percent, default 75). Drives {@code
 * LowAttendanceAlertJob}.
 */
@Component
public class LowAttendanceRuleEvaluator implements AlertRuleEvaluator {

	public static final String RULE_TYPE = "LOW_ATTENDANCE";
	private static final int ROLLING_WINDOW_DAYS = 30;
	private static final int DEFAULT_THRESHOLD_PERCENT = 75;

	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final AttendanceRepository attendanceRepository;
	private final StudentRepository studentRepository;
	private final StudentNotificationRecipientResolver recipientResolver;
	private final SchoolClock schoolClock;
	private final AttendancePercentageCalculator attendancePercentageCalculator = new AttendancePercentageCalculator();

	public LowAttendanceRuleEvaluator(StudentClassroomLinkRepository studentClassroomLinkRepository,
			AttendanceRepository attendanceRepository, StudentRepository studentRepository,
			StudentNotificationRecipientResolver recipientResolver, SchoolClock schoolClock) {
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.attendanceRepository = attendanceRepository;
		this.studentRepository = studentRepository;
		this.recipientResolver = recipientResolver;
		this.schoolClock = schoolClock;
	}

	@Override
	public String supportedRuleType() {
		return RULE_TYPE;
	}

	@Override
	public List<AlertTrigger> evaluate(AlertRule rule) {
		Long tenantId = rule.getTenantId();
		int thresholdPercent = rule.getThresholdValue() != null ? rule.getThresholdValue().intValue()
				: DEFAULT_THRESHOLD_PERCENT;
		LocalDate to = schoolClock.today(tenantId);
		LocalDate from = to.minusDays(ROLLING_WINDOW_DAYS);

		List<Long> studentIds = studentClassroomLinkRepository.findDistinctStudentIdsByTenantId(tenantId);
		if (studentIds.isEmpty()) {
			return List.of();
		}
		Map<Long, List<AttendanceTally>> talliesByStudentId = attendanceRepository
				.tallyByStudent(tenantId, studentIds, from, to, true, Set.of()).stream()
				.collect(Collectors.groupingBy(AttendanceTally::studentId));

		List<Long> belowThresholdStudentIds = new ArrayList<>();
		Map<Long, AttendancePercentage> percentageByStudentId = new HashMap<>();
		for (Long studentId : studentIds) {
			List<AttendanceTally> tallies = talliesByStudentId.get(studentId);
			if (tallies == null) {
				continue;
			}
			AttendancePercentage attendancePercentage = attendancePercentageCalculator
					.calculate(AttendanceTally.byStatus(tallies));
			if (attendancePercentage.percentage().compareTo(BigDecimal.valueOf(thresholdPercent)) < 0) {
				belowThresholdStudentIds.add(studentId);
				percentageByStudentId.put(studentId, attendancePercentage);
			}
		}
		if (belowThresholdStudentIds.isEmpty()) {
			return List.of();
		}

		List<AlertTrigger> triggers = new ArrayList<>();
		for (Student student : studentRepository.findAllByIdInAndTenantId(belowThresholdStudentIds, tenantId)) {
			buildTrigger(tenantId, student, percentageByStudentId.get(student.getId())).ifPresent(triggers::add);
		}
		return triggers;
	}

	private Optional<AlertTrigger> buildTrigger(Long tenantId, Student student,
			AttendancePercentage attendancePercentage) {
		return recipientResolver.resolve(tenantId, student)
				.map(userId -> {
					String studentName = student.getFirstName() + " " + student.getLastName();
					String percentage = attendancePercentage.percentage().toPlainString();
					return new AlertTrigger(userId, "Low Attendance Alert",
							studentName + "'s attendance over the last " + ROLLING_WINDOW_DAYS + " days is "
									+ percentage + "%.",
							Map.of(
									"studentName", studentName,
									"percentage", percentage,
									"windowDays", String.valueOf(ROLLING_WINDOW_DAYS)),
							NotificationPriority.HIGH);
				});
	}
}

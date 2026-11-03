package com.altafjava.school.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.alert.AlertDispatchService;
import com.altafjava.platform.application.service.report.provider.ReportDataProvider;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.alert.AttendanceNotMarkedRuleEvaluator;
import com.altafjava.school.application.alert.ExamScheduleReminderRuleEvaluator;
import com.altafjava.school.application.alert.FeeDefaultRiskRuleEvaluator;
import com.altafjava.school.application.alert.FeeOverdueReminderRuleEvaluator;
import com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluator;
import com.altafjava.school.application.alert.LibraryOverdueRuleEvaluator;
import com.altafjava.school.application.alert.LowAttendanceRuleEvaluator;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.event.repository.EventRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * School-wide summary for the PRINCIPAL role: one aggregate row (not per-record detail), computed
 * from single COUNT/SUM queries — never by looping per-student, which would both be slow at scale
 * and (for grade/GPA data) collide with {@code StudentDataAccessGuard}'s per-record ownership
 * check, which this role-scoped aggregate has no need to go through.
 */
@Component
public class PrincipalDashboardDataProvider implements ReportDataProvider {

	private static final int ATTENDANCE_WINDOW_DAYS = 30;

	private static final List<String> ALERT_RULE_TYPES = List.of(
			LowAttendanceRuleEvaluator.RULE_TYPE,
			FeePaymentReminderRuleEvaluator.RULE_TYPE,
			FeeOverdueReminderRuleEvaluator.RULE_TYPE,
			FeeDefaultRiskRuleEvaluator.RULE_TYPE,
			ExamScheduleReminderRuleEvaluator.RULE_TYPE,
			AttendanceNotMarkedRuleEvaluator.RULE_TYPE,
			LibraryOverdueRuleEvaluator.RULE_TYPE);

	private final StudentRepository studentRepository;
	private final AttendanceRepository attendanceRepository;
	private final EventRepository eventRepository;
	private final AlertDispatchService alertDispatchService;
	private final SchoolClock schoolClock;

	public PrincipalDashboardDataProvider(StudentRepository studentRepository,
			AttendanceRepository attendanceRepository, EventRepository eventRepository,
			AlertDispatchService alertDispatchService, SchoolClock schoolClock) {
		this.studentRepository = studentRepository;
		this.attendanceRepository = attendanceRepository;
		this.eventRepository = eventRepository;
		this.alertDispatchService = alertDispatchService;
		this.schoolClock = schoolClock;
	}

	@Override
	public List<Map<String, Object>> fetchData(Map<String, Object> parameters) {
		Long tenantId = TenantContext.getCurrentTenantId();
		LocalDateTime now = schoolClock.now(tenantId);
		LocalDate today = now.toLocalDate();
		LocalDate windowStart = today.minusDays(ATTENDANCE_WINDOW_DAYS);

		long activeStudentCount = studentRepository.countByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE,
				tenantId);
		long markedCount = attendanceRepository.countByTenantIdAndAttendanceDateBetween(tenantId, windowStart, today);
		long presentCount = attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(tenantId,
				windowStart, today, AttendanceStatus.PRESENT);
		long upcomingEventCount = eventRepository.countByTenantIdAndActiveTrueAndEventDateAfter(tenantId,
				now);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("activeStudentCount", activeStudentCount);
		row.put("attendancePercentageLast30Days", attendancePercentage(markedCount, presentCount));
		row.put("upcomingEventCount", upcomingEventCount);
		row.put("activeAlertCounts", activeAlertCounts(tenantId));
		return List.of(row);
	}

	// Read-only dry-run — the same evaluators the real alert jobs use, without sending anything.
	private Map<String, Integer> activeAlertCounts(Long tenantId) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (String ruleType : ALERT_RULE_TYPES) {
			counts.put(ruleType, alertDispatchService.evaluate(tenantId, ruleType).size());
		}
		return counts;
	}

	private BigDecimal attendancePercentage(long markedCount, long presentCount) {
		if (markedCount == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(presentCount)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(markedCount), 2, RoundingMode.HALF_UP);
	}
}

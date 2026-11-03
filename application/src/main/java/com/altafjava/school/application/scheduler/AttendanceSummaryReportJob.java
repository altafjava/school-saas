package com.altafjava.school.application.scheduler;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.scheduler.support.TenantAdminNotifier;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs weekly on Monday at 08:00 tenant-local time.
 * Generates a weekly attendance summary report and sends it to tenant admins.
 */
@Slf4j
@Component
@ScheduledJob(name = "AttendanceSummaryReport", group = "school", description = "Generates weekly attendance summary report for tenant admins", cronExpression = "0 0 8 ? * MON", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class AttendanceSummaryReportJob implements JobExecutionStrategy {

	private final AttendanceRepository attendanceRepository;
	private final TenantAdminNotifier tenantAdminNotifier;
	private final SchoolClock schoolClock;

	public AttendanceSummaryReportJob(AttendanceRepository attendanceRepository,
			TenantAdminNotifier tenantAdminNotifier, SchoolClock schoolClock) {
		this.attendanceRepository = attendanceRepository;
		this.tenantAdminNotifier = tenantAdminNotifier;
		this.schoolClock = schoolClock;
	}

	@Override
	public String jobName() {
		return "AttendanceSummaryReport";
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		log.info("action=attendance-summary-report tenantId={} executionId={}", tenantId, ctx.executionId());

		LocalDate to = schoolClock.today(tenantId);
		LocalDate from = to.minusDays(7);
		long present = countByStatus(tenantId, from, to, AttendanceStatus.PRESENT);
		long absent = countByStatus(tenantId, from, to, AttendanceStatus.ABSENT);
		long late = countByStatus(tenantId, from, to, AttendanceStatus.LATE);
		long excused = countByStatus(tenantId, from, to, AttendanceStatus.EXCUSED);

		String message = String.format(
				"Attendance summary for %s to %s — Present: %d, Absent: %d, Late: %d, Excused: %d.",
				from, to, present, absent, late, excused);
		int notifiedCount = tenantAdminNotifier.notifyAll(tenantId, "Weekly Attendance Summary", message);

		log.info("action=attendance-summary-report-complete tenantId={} notifiedCount={}", tenantId, notifiedCount);
		return new JobExecutionResult.Success(
				Map.of("present", present, "absent", absent, "late", late, "excused", excused,
						"notifiedCount", notifiedCount),
				null);
	}

	private long countByStatus(Long tenantId, LocalDate from, LocalDate to, AttendanceStatus status) {
		return attendanceRepository.countByTenantIdAndAttendanceDateBetweenAndStatus(tenantId, from, to, status);
	}
}

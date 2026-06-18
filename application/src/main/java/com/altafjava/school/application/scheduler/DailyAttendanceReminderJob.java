package com.altafjava.school.application.scheduler;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;

/**
 * Runs daily at 07:00 tenant-local time.
 * Reminds teachers in each tenant to mark attendance for the day.
 * TenantContext is set by the platform scheduler before execute() is called.
 */
@Component("schoolDailyAttendanceReminderJob")
@ScheduledJob(name = "DailyAttendanceReminder", group = "school", description = "Reminds teachers to mark attendance", cronExpression = "0 0 7 * * ?", tenantScoped = true, retryEnabled = true, maxRetries = 2)
public class DailyAttendanceReminderJob implements JobExecutionStrategy {

	private static final Logger log = LoggerFactory.getLogger(DailyAttendanceReminderJob.class);

	@Override
	public String jobName() {
		return "DailyAttendanceReminder";
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
		log.info("action=attendance-reminder tenantId={} executionId={}", ctx.tenantId(), ctx.executionId());
		// Phase 5 scope: platform wiring verification — notification dispatch added in later iterations
		return new JobExecutionResult.Success(Map.of("remindedCount", 0), null);
	}
}
